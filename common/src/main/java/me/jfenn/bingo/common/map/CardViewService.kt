package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IMapService
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.time.Duration.Companion.seconds

class CardViewService(
    private val server: MinecraftServer,
    private val config: BingoConfig,
    private val options: BingoOptions,
    private val state: BingoState,
    private val teamService: TeamService,
    private val mapService: IMapService,
    private val mapRenderService: MapRenderService,
    private val permissions: PermissionsIntegration,
    private val textProvider: TextProvider,
    private val packets: ServerPacketEvents,
) {

    companion object {
        private val FLASHING_DURATION = 4.seconds
    }

    /**
     * True if the Bingo Card HUD packets are supported by the player's client
     * False if the packets are not supported, or if the player's supported packets are not yet known
     */
    fun supportsCardHud(player: ServerPlayerEntity): Boolean {
        return config.supportClientHud &&
                packets.cardDisplayV1.isSupported(player)
    }

    /**
     * Send an empty CardDisplayPacket to clear the HUD state
     */
    fun sendClearDisplayPacket(player: ServerPlayerEntity) {
        packets.cardDisplayV1.send(player, CardDisplayPacket(
            teamInfo = emptyMap(),
            display = emptyList(),
        ))
    }

    fun sendUpdatePackets(player: ServerPlayerEntity) {
        if (!supportsCardHud(player))
            return

        val teams = state.getRegisteredTeams()
        val displayTeams = (teams + null).filter { isViewingCard(player, it) }

        val packet = CardDisplayPacket(
            teamInfo = teams.associate { it.key to it.textColor },
            display = displayTeams.map { it?.key },
        )

        packets.cardDisplayV1.send(player, packet)

        for (team in displayTeams) {
            val map = team
                ?.let { teamService.getTeamMap(it) }
                ?: state.getPreviewMap(mapService)

            val view = map.view
            val updatePacket = CardUpdatePacket(view)
            when {
                packets.cardUpdateV4.send(player, updatePacket) -> {}
                packets.cardUpdateV3.send(player, updatePacket) -> {}
                packets.cardUpdateV2.send(player, updatePacket) -> {}
                packets.cardUpdateV1.send(player, updatePacket) -> {}
            }
        }
    }

    fun isViewingPreviewCard(player: ServerPlayerEntity)
        = state.state == GameState.PREGAME && options.showPreviewCard && (state.isLobbyMode || teamService.isPlaying(player))

    fun isViewingTeamCard(player: ServerPlayerEntity, team: BingoTeam)
        = state.state == GameState.PLAYING && team.includesPlayer(player)

    fun isViewingSpectatorCards(player: ServerPlayerEntity) = when (state.state) {
        // if the state is not POSTGAME, players must have permission to receive the spectator cards
        GameState.PLAYING -> !teamService.isPlaying(player) && permissions.hasPermission(player, Permission.SPECTATOR_VIEW_CARDS)
        GameState.POSTGAME -> true
        else -> false
    }

    fun isViewingCard(player: ServerPlayerEntity, team: BingoTeam?): Boolean {
        return (team == null && isViewingPreviewCard(player)) ||
                (team != null && isViewingTeamCard(player, team)) ||
                (team != null && isViewingSpectatorCards(player))
    }

    private fun getTileState(
        team: BingoTeam?,
        entry: BingoCardEntry,
        objective: BingoObjective,
    ) : CardTile {
        val isFreeSpace = objective is BingoObjective.FreeSpace
        val isHidden = options.isHiddenItemsMode && (team == null || !objective.hasSeen(team.key)) && !isFreeSpace
        val isLocked = options.isLockoutMode && objective.hasAnyAchieved() && team != null && !objective.hasAchieved(team.key)
        val isAchieved = team != null && objective.hasAchieved(team.key)

        return CardTile(
            item = if (isHidden) null else objective.visualItem(entry.tier),
            itemTier = entry.tier.takeIf { !options.isHiddenItemsMode || options.showHiddenTiers || !isHidden },
            name = objective.display.name.takeIf { !isHidden },
            lore = objective.display.lore.takeIf { !isHidden } ?: emptyList(),
            decoration = when {
                isFreeSpace -> CardTile.Decoration.FREE_SPACE
                objective is BingoObjective.AdvancementEntry ||
                        objective is BingoObjective.OpponentEntry -> CardTile.Decoration.ADVANCEMENT
                objective is BingoObjective.OneOfEntry ||
                        objective is BingoObjective.AllOfEntry -> CardTile.Decoration.MULTI_ITEM
                objective is BingoObjective.InverseEntry -> CardTile.Decoration.FORBIDDEN
                else -> null
            }.takeIf { isFreeSpace || !isHidden },
            isHidden = isHidden,
            isLocked = isLocked,
            isFlashing = objective.isFlashing(team?.key, FLASHING_DURATION) && (server.ticks / 10) % 2 == 0,
            isAchieved = isAchieved,
            progress = when {
                isAchieved -> 1f
                !isLocked && team != null -> objective.getProgress(team.key)
                else -> 0f
            },
            teamKeys = if (options.showOtherTeamCompletions && state.state != GameState.POSTGAME)
                objective.teamsAchieved.keys.filter { it != team?.key }
            else emptyList(),
        )
    }

    fun getMapView(team: BingoTeam?): CardView {
        return CardView(
            teamKey = team?.key,
            teamColor = team?.mapColor,
            teamName = team
                ?.let { textProvider.string(StringKey.CardTeamCardTitle, textProvider.string(it.name)) }
                ?: textProvider.string(StringKey.CardPreviewCard),
            tiles = state.card.entries
                .map { entry ->
                    val goal = state.card.objectives[entry.objectiveId]
                    goal?.let { getTileState(team, entry, it) }
                        ?: CardTile.EMPTY
                }
                .toMutableList(),
            players = team?.players?.toList() ?: emptyList(),
        )
    }

    fun updateCard(
        team: BingoTeam?,
        forceNotFlashing: Boolean = false,
    ) {
        var view = getMapView(team)
        val map = team?.let { teamService.getTeamMap(it) } ?: state.getPreviewMap(mapService)

        if (forceNotFlashing) {
            view = view.copy(
                tiles = map.view.tiles.map {
                    it.copy(isFlashing = false)
                }.toMutableList(),
            )
        }

        mapRenderService.update(team, map, view)
        map.view = view

        for (player in server.playerManager.playerList) {
            sendUpdatePackets(player)
        }
    }

}