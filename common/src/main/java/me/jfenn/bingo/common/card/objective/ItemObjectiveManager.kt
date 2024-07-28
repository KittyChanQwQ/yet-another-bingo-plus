package me.jfenn.bingo.common.card.objective

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.OfflinePlayerCache
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.slf4j.Logger

class ItemObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val itemStackFactory: IItemStackFactory,
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    private val offlinePlayerCache: OfflinePlayerCache,
    private val objectiveRegistry: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
): IObjectiveManager {

    override fun find(id: String, state: CardGeneratorState): BingoObjective.ItemEntry? {
        // If the objective is a data ID
        (objectiveRegistry.objectives[id] as? ObjectiveData.Item)
            ?.let { data ->
                BingoObjective.ItemEntry(
                    id = id,
                    itemId = data.item,
                    displayData = data.display,
                )
            }
            ?.takeIf { init(it) }
            ?.let { return it }

        // If the objective is a direct item identifier
        return BingoObjective.ItemEntry(id = id, itemId = id).takeIf { init(it) }
    }

    private fun init(goal: BingoObjective): Boolean {
        if (goal !is BingoObjective.ItemEntry) return false

        val stack = try {
            if (!itemStackFactory.isEnabledInWorld(goal.itemId, server))
                throw IllegalArgumentException()

            itemStackFactory.createStack(goal.itemId, count = goal.itemCount)
        } catch (e: Throwable) {
            return false
        }

        goal.itemStack = stack
        goal.display = goal.displayData
            ?.let { objectiveDisplayService.resolve(goal.id, it) }
            ?.let { it.copy(name = it.name?.withCount(goal.itemCount)) }
            ?: ObjectiveDisplay.Resolved(
                name = stack.displayName.withCount(goal.itemCount),
                item = stack
            )

        return true
    }

    private fun Text.withCount(count: Int): Text {
        return if (count > 1) {
            Text.empty().append(this).append(" x${count}")
        } else {
            this
        }
    }

    override fun init(card: BingoCard) {
        for (goal in card.objectivesByInstance<BingoObjective.ItemEntry>()) {
            if (!init(goal)) {
                log.error("[ItemGoalManager] Unable to find item '${goal.itemId}'")
            }
        }
    }

    private fun canSatisfy(stack: IItemStack, objective: BingoObjective.ItemEntry) : Boolean {
        val objectiveStack = objective.itemStack ?: return false
        return stack.item == objectiveStack.item &&
                !stack.hasCustomTag(NBT_BINGO_IGNORE) &&
                stack.count >= objective.itemCount
    }

    override fun tick(card: BingoCard) {
        for (player in playerManager.getPlayers()) {
            val team = teamService.getPlayerTeam(player.player) ?: continue

            // don't update teams that have already won
            if (team.isWinner()) continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val goalId = stack.identifier.toString()
                val goal = card.objectives[goalId] ?: continue
                if (goal !is BingoObjective.ItemEntry) continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (canSatisfy(stack, goal)) {
                    goal.markSeen(team.key)
                } else {
                    continue
                }

                // if the gamemode is lockout, tiles cannot be collected by >1 team
                if (options.isLockoutMode && goal.hasAnyAchieved())
                    continue

                val isChanged = goal.setAchieved(team.key, true)

                // if the tile has changed, mark updatedTeams
                if (isChanged) {
                    // record the player that captured the tile
                    val profile = BingoPlayerProfile.fromPlayer(player)
                    goal.players.add(profile)
                }
            }
        }

        // if isInventoryMode, we need to check that each team still has all their items
        // and mark them as lost if not
        if (options.isInventoryMode) {
            for (goal in card.objectivesByInstance<BingoObjective.ItemEntry>()) {
                for (teamKey in goal.teams.toSet()) {
                    val team = state.teams[teamKey] ?: continue
                    // don't update teams that have already won
                    if (team.isWinner()) continue

                    val teamPlayers = team.players.map { offlinePlayerCache.getOfflinePlayer(it) }
                    val hasTeamItem = teamPlayers.any { player ->
                        player.allHeldStacks().any { canSatisfy(it, goal) }
                    }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) {
                        val isChanged = goal.setAchieved(team.key, false)
                        // if the tile has changed, mark updatedTeams
                        if (isChanged) {
                            // since the tile was lost, remove the team's players from the tile
                            goal.players.removeAll(team.players)
                        }
                    }
                }
            }
        }
    }
}