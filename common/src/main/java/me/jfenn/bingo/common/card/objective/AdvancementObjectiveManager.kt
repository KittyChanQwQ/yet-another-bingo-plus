package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.OfflinePlayerCache
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class AdvancementObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val advancementManager: IAdvancementManager,
    private val itemStackFactory: IItemStackFactory,
    private val state: BingoState,
    private val options: BingoOptions,
    private val offlinePlayerCache: OfflinePlayerCache,
) : IObjectiveManager {

    override fun find(id: String, state: CardGeneratorState): BingoObjective.AdvancementEntry? {
        return BingoObjective.AdvancementEntry(id, advancementId = id).takeIf { init(it) }
    }

    private fun init(goal: BingoObjective): Boolean {
        if (goal !is BingoObjective.AdvancementEntry) return false

        val advancement = advancementManager.getAdvancement(server, goal.advancementId)
            ?: return false

        goal.advancement = advancement
        goal.display = ObjectiveDisplay.Resolved(
            name = advancement.name
                ?.let {
                    Text.empty()
                        .append(it)
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN))
                },
            lore = advancement.description
                ?.string
                ?.split("\n")
                ?.map { Text.literal(it) }
                ?: emptyList(),
            item = advancement.displayIcon?.let { itemStackFactory.forStack(it) },
        )

        return true
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            if (!init(goal)) {
                log.error("[AdvancementGoalManager] Unable to find advancement '${goal.advancementId}'")
            }
        }
    }

    override fun tick(card: BingoCard) {
        val now = Clock.System.now()

        for (objective in card.objectivesByInstance<BingoObjective.AdvancementEntry>()) {
            val advancement = objective.advancement ?: continue

            for (team in state.getRegisteredTeams()) {
                // don't update teams that have already won
                if (team.isWinner()) continue

                val players = team.players.map { offlinePlayerCache.getOfflinePlayer(it) }

                val isAnyObtained = players.any {
                    advancementManager.isAnyObtained(it.player, advancement)
                }
                if (isAnyObtained) {
                    objective.markSeen(team.key)
                }

                val player = players.find {
                    advancementManager.isDone(it.player, advancement)
                }

                // if the advancement is finished, and the tile is not locked
                if (player != null && !(options.isLockoutMode && objective.hasAnyAchieved())) {
                    val isChanged = objective.setAchieved(team.key, true)

                    // if the tile has changed, mark updatedTeams
                    if (isChanged) {
                        // record the player that captured the tile
                        val profile = BingoPlayerProfile.fromPlayer(player)
                        objective.addPlayer(profile, now)
                    }
                }

                // if noone on the team has the advancement anymore, then mark it as lost...
                if (player == null) {
                    val isChanged = objective.setAchieved(team.key, false)
                    // if the tile has changed, mark updatedTeams
                    if (isChanged) {
                        // since the tile was lost, remove the team's players from the tile
                        objective.removePlayers(team.players)
                    }
                }
            }
        }
    }
}