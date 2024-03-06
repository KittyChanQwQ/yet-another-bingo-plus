package me.jfenn.bingo.common.map

import me.jfenn.bingo.common.card.BingoCardEntry
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.server.MinecraftServer
import kotlin.time.Duration.Companion.seconds

class CardViewService(
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val teamService: TeamService,
) {

    companion object {
        private val FLASHING_DURATION = 4.seconds
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
            isMultiItem = entry is BingoCardEntry.MultiEntry,
            isHidden = isHidden,
            isLocked = options.isLockoutMode && entry.teams.isNotEmpty() && team != null && !entry.hasAchieved(team),
            isFlashing = entry.isFlashing(team, FLASHING_DURATION) && (server.ticks / 10) % 2 == 0,
            isAchieved = team != null && entry.hasAchieved(team),
            teamColors = if (options.showOtherTeamCompletions)
                entry.teams.filter { it != team?.key }.mapNotNull { state.teams[it]?.textColor }
            else emptyList(),
        )
    }

    fun getMapView(team: BingoTeam?): CardView {
        return CardView(
            teamKey = team?.key,
            teamColor = team?.textColor,
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