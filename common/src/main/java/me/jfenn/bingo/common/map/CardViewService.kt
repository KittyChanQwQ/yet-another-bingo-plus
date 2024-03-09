package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.PACKET_CARD_DISPLAY
import me.jfenn.bingo.common.PACKET_CARD_UPDATE
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import kotlin.time.Duration.Companion.seconds

class CardViewService(
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val config: BingoConfig,
    private val state: BingoState,
    private val teamService: TeamService,
    private val permissions: PermissionsIntegration,
) {

    companion object {
        private val FLASHING_DURATION = 4.seconds
    }

    /**
     * True if the player's supported packets are known to the server
     * False if the player is still connecting
     */
    fun isNetworkReady(player: ServerPlayerEntity): Boolean {
        return ServerPlayNetworking.getSendable(player).isNotEmpty()
    }

    /**
     * True if the Bingo Card HUD packets are supported by the player's client
     * False if the packets are not supported, or if the player's supported packets are not yet known
     */
    fun supportsCardHud(player: ServerPlayerEntity): Boolean {
        return config.supportClientHud &&
                ServerPlayNetworking.canSend(player, PACKET_CARD_DISPLAY) &&
                ServerPlayNetworking.canSend(player, PACKET_CARD_UPDATE)
    }

    fun isViewingPreviewCard()
        = state.state == GameState.PREGAME && options.showPreviewCard

    fun isViewingTeamCard(player: ServerPlayerEntity, team: BingoTeam)
        = state.state == GameState.PLAYING && team.includesPlayer(player)

    fun isViewingSpectatorCards(player: ServerPlayerEntity) = when (state.state) {
        // if the state is not POSTGAME, players must have permission to receive the spectator cards
        GameState.PLAYING -> !teamService.isPlaying(player) && permissions.hasPermission(player, Permission.SPECTATOR_VIEW_CARDS)
        GameState.POSTGAME -> true
        else -> false
    }

    fun isViewingCard(player: ServerPlayerEntity, team: BingoTeam?): Boolean {
        return (team == null && isViewingPreviewCard()) ||
                (team != null && isViewingTeamCard(player, team)) ||
                (team != null && isViewingSpectatorCards(player))
    }

    private fun getTileState(
        team: BingoTeam?,
        entry: BingoCardEntry,
    ) : CardTile {
        val isFreeSpace = entry is BingoCardEntry.FreeSpace
        val isHidden = options.isHiddenItemsMode && (team == null || !entry.hasSeen(team)) && !isFreeSpace

        return CardTile(
            item = entry.visualItem.takeIf { !isHidden },
            itemTier = entry.tier.takeIf { options.showHiddenTiers },
            isFreeSpace = isFreeSpace,
            isAdvancement = !isHidden && (
                    entry is BingoCardEntry.AdvancementEntry ||
                            (entry as? BingoCardEntry.MultiEntry)?.innerEntries?.firstOrNull() is BingoCardEntry.AdvancementEntry
            ),
            isMultiItem = !isHidden && entry is BingoCardEntry.MultiEntry,
            isHidden = isHidden,
            isLocked = options.isLockoutMode && entry.teams.isNotEmpty() && team != null && !entry.hasAchieved(team),
            isFlashing = entry.isFlashing(team, FLASHING_DURATION) && (server.ticks / 10) % 2 == 0,
            isAchieved = team != null && entry.hasAchieved(team),
            teamColors = if (options.showOtherTeamCompletions)
                entry.teams.filter { it != team?.key }.mapNotNull { state.teams[it]?.formatting }
            else emptyList(),
        )
    }

    fun getMapView(team: BingoTeam?): CardView {
        return CardView(
            teamKey = team?.key,
            teamColor = team?.mapColor,
            teamName = team?.name?.let { "${it.formatTitle()} Team" } ?: "Preview Card",
            tiles = state.card.entries
                .map { entry -> getTileState(team, entry) }
                .toMutableList()
        )
    }

    fun needsUpdate(
        map: BingoMap,
        view: CardView,
    ): Boolean {
        return map.view != view
    }

}