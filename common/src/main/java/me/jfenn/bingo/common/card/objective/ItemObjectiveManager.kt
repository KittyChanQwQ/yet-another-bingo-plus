package me.jfenn.bingo.common.card.objective

import kotlinx.datetime.Clock
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.card.BingoCard
import me.jfenn.bingo.common.card.CardGeneratorState
import me.jfenn.bingo.common.card.data.ObjectiveData
import me.jfenn.bingo.common.card.data.ObjectiveRegistryState
import me.jfenn.bingo.common.map.CardTile
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import org.slf4j.Logger
import kotlin.math.max

class ItemObjectiveManager(
    private val log: Logger,
    private val server: MinecraftServer,
    private val itemStackFactory: IItemStackFactory,
    private val playerManager: IPlayerManager,
    private val objectiveRegistry: ObjectiveRegistryState,
    private val objectiveDisplayService: ObjectiveDisplayService,
    private val objectiveService: ObjectiveService,
): IObjectiveManager {
    override fun list(): Iterable<String> {
        val dataKeys = objectiveRegistry.objectives
            .filterValues { it is ObjectiveData.Item }
            .keys
        return itemStackFactory.listItems(server) + dataKeys
    }

    override fun find(id: String, state: CardGeneratorState): BingoObjective.ItemEntry? {
        // If the objective is a data ID
        (objectiveRegistry.objectives[id] as? ObjectiveData.Item)
            ?.let { data ->
                BingoObjective.ItemEntry(
                    id = id,
                    data = data,
                    itemCount = data.count.resolve(state),
                )
            }
            ?.takeIf { init(it, isDataObjective = true) }
            ?.let { return it }

        // If the objective is a direct item identifier
        return BingoObjective.ItemEntry(
            id = id,
            data = ObjectiveData.Item(item = id),
        ).takeIf { init(it) }
    }

    private fun init(objective: BingoObjective, isDataObjective: Boolean = false): Boolean {
        if (objective !is BingoObjective.ItemEntry) return false

        val stack = try {
            if (!itemStackFactory.isEnabledInWorld(objective.itemId, server))
                throw IllegalArgumentException()

            itemStackFactory.createStack(objective.itemId, count = objective.itemCount)
        } catch (e: Throwable) {
            if (isDataObjective) {
                log.error("[ItemObjectiveManager] ${objective.id}: Item ${objective.itemId} could not be found.")
            }
            return false
        }

        val isNbtSuccessful = stack.setNbtString(objective.itemNbt)
        val isComponentsSuccessful = objective.itemComponents?.let { stack.setComponentsString(it) } ?: true

        if (!isNbtSuccessful || !isComponentsSuccessful) {
            log.error("[ItemObjectiveManager] Item ${objective.itemId} NBT is not in a valid format")
            return false
        }

        objective.itemStack = stack
        objective.itemCount = objective.itemCount.coerceIn(1, stack.maxCount)

        val display = ObjectiveDisplay.Resolved(
            name = stack.displayName,
            item = stack,
            decoration = when {
                objective.itemNbt != null || !objective.itemComponents.isNullOrEmpty() -> CardTile.Decoration.ADVANCEMENT
                else -> CardTile.Decoration.NONE
            }
        )
        objective.display = objectiveDisplayService.resolve(objective.id, objective.data.display, display)
            .let { it.copy(name = it.name?.withCount(objective.itemCount)) }

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

    private fun canSatisfyPartial(stack: IItemStack, objective: BingoObjective.ItemEntry) : Boolean {
        val objectiveStack = objective.itemStack ?: return false
        return stack.item == objectiveStack.item &&
                !stack.hasCustomTag(NBT_BINGO_IGNORE) &&
                stack.isDataOverlapping(nbt = objective.itemNbt, components = objective.itemComponents)
    }

    private fun canSatisfy(stack: IItemStack, objective: BingoObjective.ItemEntry) : Boolean {
        return canSatisfyPartial(stack, objective) &&
                stack.count >= objective.itemCount
    }

    override fun tick(card: BingoCard) {
        val now = Clock.System.now()

        val lockedTeams = objectiveService.getLockedTeams()
        val lockedTeamKeys = lockedTeams.map { it.key }.toSet()
        val lockedTeamPlayers = lockedTeams.flatMap { it.players }.map { it.uuid }.toSet()

        for (objective in card.objectivesByInstance<BingoObjective.ItemEntry>()) {
            objectiveService.updateTeamsOnceAchieved(objective)

            // team progress is recalculated on each tick... except for teams that are locked (once a card is complete)
            objective.teamsProgress = objective.teamsProgress
                .filterKeys { lockedTeamKeys.contains(it) }
                .toMutableMap()
        }

        // first, we need to check that each player still has all their items
        // and remove them from playersHolding if not
        for (objective in card.objectivesByInstance<BingoObjective.ItemEntry>()) {
            for (player in playerManager.getPlayers()) {
                // if the player has no objectives to lose, skip
                if (!objective.playersHolding.containsKey(player.uuid)) continue
                // if the player is on a locked team, skip
                if (lockedTeamPlayers.contains(player.uuid)) continue

                val hasItem = player.allHeldStacks().any { canSatisfy(it, objective) }

                // if the player does not have the item anymore, mark it lost
                if (!hasItem) {
                    objective.playersHolding.remove(player.uuid)
                }
            }
        }

        for ((team, player) in objectiveService.getTeamPlayers()) {
            // if the team is locked, skip
            if (lockedTeamKeys.contains(team.key)) continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get any objectives for the itemId, if it is on the card
                val itemId = stack.identifier.toString()
                val objectives = card.itemObjectives[itemId] ?: continue

                for (objective in objectives) {
                    // if the item hasn't been achieved and can be satisfied, mark it as collected
                    if (canSatisfyPartial(stack, objective)) {
                        objective.teamsSeen += team.key
                    } else {
                        continue
                    }

                    val progress = stack.count / objective.itemCount.coerceAtLeast(1).toFloat()
                    objective.teamsProgress.compute(team.key) { _, prev -> max(prev ?: 0f, progress) }

                    if (!canSatisfy(stack, objective))
                        continue

                    if (!objective.playersHolding.containsKey(player.uuid)) {
                        objective.playersHolding[player.uuid] = BingoObjectiveCapture(
                            team = team.key,
                            player = BingoPlayerProfile.fromPlayer(player),
                            instant = now,
                        )
                    }
                }
            }
        }

        for (objective in card.objectivesByInstance<BingoObjective.ItemEntry>()) {
            val players = objectiveService.getPlayers(
                playersHolding = objective.playersHolding,
                players = objective.players,
            ).toMutableMap()
            val teams = objectiveService.getTeams(
                players = players
            ).toMutableSet()

            objectiveService.update(
                objective = objective,
                players = players,
                teams = teams,
            )
        }
    }
}