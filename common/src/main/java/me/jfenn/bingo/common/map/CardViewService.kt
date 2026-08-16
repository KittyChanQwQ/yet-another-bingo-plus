package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.CardInventory
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.display.ItemStyle
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.seconds
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IMapService
import me.jfenn.bingo.platform.IMinecraftServer
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.player.IContainerMenu
import me.jfenn.bingo.platform.text.Formatting
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.stats.sql.Game
import java.util.*

internal class CardViewService(
    private val server: IMinecraftServer,
    private val config: BingoConfig,
    private val options: BingoOptions,
    private val state: BingoState,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    private val mapService: IMapService,
    private val mapRenderService: MapRenderService,
    private val permissions: IPermissionsApi,
    private val textProvider: TextProvider,
    private val packets: ServerPacketEvents,
    private val itemStackFactory: IItemStackFactory,
    private val containerMenuFactory: IContainerMenu.Factory,
) {

    companion object {
        private val FLASHING_DURATION = 4.seconds
    }

    /**
     * True if the Bingo Card HUD packets are supported by the player's client
     * False if the packets are not supported, or if the player's supported packets are not yet known
     */
    fun supportsCardHud(player: IPlayerHandle): Boolean {
        return config.supportClientHud &&
                (packets.cardDisplayV1.isSupported(player))
    }

    /**
     * Send an empty CardDisplayPacket to clear the HUD state
     */
    fun sendClearDisplayPacket(player: IPlayerHandle) {
        when {
            packets.cardResetV1.send(player, CardResetPacket()) -> {}
            else -> sendCardDisplayPacket(player, emptyMap())
        }
    }

    private fun sendCardDisplayPacket(
        player: IPlayerHandle,
        displayCards: Map<BingoTeamKey?, CardDisplay>,
    ) {
        val packet = CardDisplayPacket(
            display = displayCards,
        )

        when {
            packets.cardDisplayV1.send(player, packet) -> {}
        }
    }

    private fun sendEntireCardTilesPacket(
        player: IPlayerHandle,
        view: CardView,
    ) {
        val tilesPacket = CardTilesPacket(
            teamKey = view.teamKey,
            tiles = view.tiles
                .withIndex()
                .associate { it.index to it.value },
            shouldNotify = false,
        )

        when {
            packets.cardTilesV1.send(player, tilesPacket) -> {}
        }
    }

    fun sendCardShuffledPacket(cardId: UUID) {
        val affectedTeams = buildList {
            if (state.getActiveCard().id == cardId)
                add(null)
            addAll(
                state.getRegisteredTeams()
                    .filter { it.cardId == cardId }
            )
        }

        for (player in playerManager.getPlayers()) {
            val playerTeams = affectedTeams
                .filter { isViewingCard(player, it) }

            for (team in playerTeams) {
                val packet = CardShuffledPacket(team?.key)
                packets.cardShuffledV1.send(player, packet)
            }
        }
    }

    fun sendUpdatePackets(player: IPlayerHandle) {
        if (!supportsCardHud(player))
            return

        val teams = state.getRegisteredTeams()
        val displayTeams = (teams + null)
            .filter { isViewingCard(player, it) }

        val isViewingSpectatorCards = displayTeams.size > 1
        val displayCards = displayTeams.mapNotNull { team ->
            val map = team
                ?.let { teamService.getTeamMap(it) }
                ?: state.getPreviewMap(mapService)

            // If the map view has no tiles, update it first!
            // (this is necessary to populate the display info)
            if (map.view?.tiles?.isEmpty() != true) {
                updateCard(team)
            }

            val display = when {
                // When the player is viewing multiple cards, use player names on the card titles
                // (otherwise, it only shows the team name)
                isViewingSpectatorCards -> map.view?.display?.copy(
                    teamName = getMapName(team, playerName = true)
                )
                else -> map.view?.display
            }

            display?.let { Pair(team?.key, it) }
        }

        sendCardDisplayPacket(player, displayCards.toMap())

        for ((key, display) in displayCards) {
            val map = state.teams[key]
                ?.let { teamService.getTeamMap(it) }
                ?: state.getPreviewMap(mapService)

            map.view?.copy(display = display)
                ?.let { sendEntireCardTilesPacket(player, it) }
        }
    }

    fun isViewingPreviewCard(player: IPlayerHandle) =
        state.state == GameState.PREGAME && options.showPreviewCard && state.isLobbyMode && !teamService.isPlaying(
            player
        )

    fun isViewingTeamCard(player: IPlayerHandle, team: BingoTeam) =
        (state.state == GameState.PLAYING || (options.showPreviewCard && state.state != GameState.POSTGAME)) && team.includesPlayer(player)

    fun isViewingSpectatorCards(player: IPlayerHandle): Boolean {
        val team = teamService.getPlayerTeam(player)
        val canViewSpectatorCards = (team == null || team.isWinner()) && permissions.hasPermission(
            player,
            Permission.SPECTATOR_VIEW_CARDS
        )
        return when (state.state) {
            // if the state is not POSTGAME, players must have permission to receive the spectator cards
            GameState.STARTING,
            GameState.PRELOADING,
            GameState.LOADING,
            GameState.COUNTDOWN -> options.showPreviewCard && canViewSpectatorCards
            GameState.PLAYING -> canViewSpectatorCards
            GameState.POSTGAME -> true
            else -> false
        }
    }

    fun isViewingCard(player: IPlayerHandle, team: BingoTeam?): Boolean {
        return (team == null && isViewingPreviewCard(player)) ||
                (team != null && isViewingTeamCard(player, team)) ||
                (team != null && isViewingSpectatorCards(player))
    }

    fun getPlayerCard(player: IPlayerHandle?): BingoCard {
        return player?.let { teamService.getPlayerTeam(it) }
            ?.let { state.getCard(it) }
            ?: state.getActiveCard()
    }

    private fun getTileState(
        team: BingoTeam?,
        entry: BingoCardEntry,
        objective: BingoObjective,
        card: BingoCard,
    ): CardTile {
        // Contains the current state of the card; in case the team is the winner, their card reference will be
        // the completed card state rather than the actual state for in-game teams
        val actualCard = state.getCard(card.id)
        val actualObjective = actualCard?.objectives?.get(objective.id)

        val isFreeSpace = objective is BingoObjective.FreeSpace
        val isHidden = card.options.isHiddenItemsMode &&
                (team == null || !objective.hasSeen(team.key)) &&
                !isFreeSpace &&
                // If the team has won or the state is POSTGAME, tiles are no longer hidden
                state.state != GameState.POSTGAME &&
                team?.isWinner() != true
        val isLocked =
            card.options.isLockoutMode && objective.hasAnyAchieved() && team != null && !objective.hasTeamAchieved(team.key)
        val isAchieved = team != null && objective.hasTeamAchieved(team.key)

        val image = (if (isHidden) CardTileImage.EMPTY else entry.display.toCardTileImage())
            .copy(isAchieved = isAchieved)

        val imageListObjectives = when (objective) {
            is BingoObjective.SomeOfEntry -> objective.someOfObjectives
                .takeIf { !isHidden && objective.display.itemStyle in arrayOf(ItemStyle.ONE_OF, ItemStyle.ALL_OF) }
                ?.mapNotNull { card.objectives[it] }
                ?.sortedBy { innerObjective ->
                    // prefer displaying objectives that are not yet achieved
                    val isInnerAchieved = team?.key?.let { innerObjective.hasTeamAchieved(it) } ?: false
                    if (isInnerAchieved) 1 else 0
                }
            else -> emptyList()
        }

        val imageList = imageListObjectives
            ?.map {
                val isAchieved = team != null && it.hasTeamAchieved(team.key)
                it.display.toCardTileImage().copy(isAchieved = isAchieved)
            }
            .orEmpty()

        val lore = if (team != null && objective is BingoObjective.SomeOfEntry && objective.valueMin > 1) {
            val count = objective.someOfObjectives.count { card.objectives[it]?.hasTeamAchieved(team.key) == true }
            entry.display.lore.orEmpty() + textProvider.string(
                StringKey.CardObjectiveProgress,
                count.toString(),
                objective.valueMin.toString(),
            ).formatted(Formatting.GRAY)
        } else entry.display.lore

        val action = when (
            val actionObjective = imageListObjectives?.firstOrNull() ?: objective
        ) {
            is BingoObjective.ItemEntry -> {
                actionObjective.itemStack?.let { CardTileAction.Item(it) }
                    ?: CardTileAction.None
            }
            is BingoObjective.AdvancementEntry -> {
                CardTileAction.Advancement(actionObjective.advancementId)
            }
            else -> CardTileAction.None
        }

        val isFlashing = objective.isFlashing(team?.key, FLASHING_DURATION)

        return CardTile(
            id = objective.id.takeIf { !isHidden },
            image = image,
            imageList = imageList,
            action = action.takeIf { !isHidden } ?: CardTileAction.None,
            itemTier = entry.tier.takeIf { !card.options.isHiddenItemsMode || options.showHiddenTiers || !isHidden },
            name = entry.display.name.takeIf { !isHidden },
            lore = lore.takeIf { !isHidden } ?: emptyList(),
            isHidden = isHidden,
            isLocked = isLocked,
            isFlashingOnMap = isFlashing && (server.ticks / 10) % 2 == 0,
            isFlashing = isFlashing,
            progress = when {
                isAchieved -> 1f
                !isLocked && team != null -> objective.getProgress(team.key)
                else -> 0f
            },
            teamKeys = actualObjective
                ?.takeIf { options.showCompletedItems && state.state != GameState.POSTGAME }
                ?.teamsAchieved?.keys
                ?.filter { it != team?.key }
                ?: emptyList(),
        )
    }

    private fun getMapName(team: BingoTeam?, playerName: Boolean = false): IText {
        return team?.getName(
            textProvider,
            playerName = playerName,
            symbol = false,
            bracketed = false,
            teamNameKey = StringKey.CardTeamCardTitle
        )
            ?.resetStyle()
            ?: textProvider.string(StringKey.CardPreviewCard)
    }

    private fun getMapView(team: BingoTeam?): CardView {
        val card = team?.let { state.getCard(it) }
            ?: state.getActiveCard()

        return CardView(
            teamKey = team?.key,
            display = CardDisplay(
                teamColor = team?.mapColor,
                teamName = getMapName(team),
                players = team?.players?.toList() ?: emptyList(),
            ),
            tiles = card.entries
                .map { entry ->
                    val goal = card.objectives[entry.objectiveId]
                    goal?.let { getTileState(team, entry, it, card) }
                        ?: CardTile.EMPTY
                }
                .toMutableList(),
        )
    }

    fun updateCard(
        team: BingoTeam?,
        forceNotFlashing: Boolean = false,
    ) {
        val card = team?.let { state.getCard(it) }
            ?: state.getActiveCard()

        var currentView = getMapView(team)
        val map = team?.let { teamService.getTeamMap(it) } ?: state.getPreviewMap(mapService)

        if (forceNotFlashing) {
            currentView = currentView.copy(
                tiles = map.view?.tiles?.map {
                    it.copy(isFlashingOnMap = false)
                }.orEmpty().toMutableList(),
            )
        }

        // Determine which tiles have been changed since the last update
        val changedTiles = currentView.tiles
            .withIndex()
            .filter { (i, tile) -> tile != map.view?.tiles?.getOrNull(i) }
            .map { it.index }

        if (changedTiles.isEmpty())
            return

        // Re-draw changed tiles on the vanilla map
        map.view = currentView
        mapRenderService.update(team, map, changedTiles)

        val cardTilesPacket = CardTilesPacket(
            teamKey = team?.key,
            tiles = changedTiles.associateWith {
                currentView.tiles[it]
            },
            // prevent flickering items when messing with the lobby menu (PREGAME)
            // or when the card is rerolled (card.ticks)
            shouldNotify = state.state == GameState.PLAYING && card.ticks > 3 && !forceNotFlashing,
        )

        val players = playerManager.getPlayers()
            .filter { isViewingCard(it, team) }

        for (player in players) {
            when {
                packets.cardTilesV1.send(player, cardTilesPacket) -> {}
            }
        }
    }

    fun openCard(player: IPlayerHandle) {
        val team = teamService.getPlayerTeam(player)

        if (packets.cardOpenV1.send(player, CardOpenPacket(team?.key)))
            return

        val view = when {
            !isViewingCard(player, team) -> null
            team == null -> state.previewMap?.view
            else -> team.map?.view
        }

        if (view == null) {
            player.sendHotbarMessage(textProvider.string(StringKey.CardNoCards))
            return
        }

        containerMenuFactory.createInventory(
            inventory = CardInventory(view, itemStackFactory, textProvider),
            name = textProvider.string(StringKey.CardTitle),
            isInteractable = false,
            onClose = {},
        ).let { player.openMenu(it) }
    }
}