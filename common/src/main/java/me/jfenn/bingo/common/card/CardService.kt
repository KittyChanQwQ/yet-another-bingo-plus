package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListEntry
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.Logger
import kotlin.math.ceil
import kotlin.random.Random

class CardService(
    private val log: Logger,
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val tierListService: TierListState,
    private val objectiveManager: BingoObjectiveManager,
    private val tagExpansionService: TagExpansionService,
) {
    fun shuffleCard() {
        // Change the BINGO seed to a new value
        state.seed = Random.nextLong()
        // Re-generate the card
        generateCard()
    }

    fun generateCard() {
        if (!options.isValid()) return

        state.card = generate(options, state.seed)
    }

    fun replaceEntry(x: Int, y: Int, objectiveId: String) {
        val generatorState = CardGeneratorState(random = Random.Default)
        // Make sure that the objective exists
        val objective = objectiveManager.find(objectiveId, generatorState)
            ?: throw IllegalArgumentException("Could not find objective '$objectiveId'")

        replaceEntry(x, y, objective)
    }

    fun replaceEntry(x: Int, y: Int, objective: BingoObjective) {
        val generatorState = CardGeneratorState(random = Random.Default)

        // Overwrite the entry being replaced
        val entries = state.card.entries.toMutableList()
        entries[x + y * 5] = BingoCardEntry(
            objectiveId = objective.id,
            tier = null,
        )

        // Re-build the objectives map (but we don't need to check for conflicts)
        val objectives = mutableMapOf<String, BingoObjective>()
        for (entry in entries) {
            val objectiveMap = createObjectives(entry.objectiveId, generatorState) ?: continue
            objectives.putAll(objectiveMap)
        }

        // If any objectives already exist on the card, use them instead of the new instance
        // (so that partial completion/state doesn't get lost if this is used mid-game)
        for (key in objectives.keys.toSet()) {
            state.card.objectives[key]?.let {
                objectives[key] = it
            }
        }

        objectives[objective.id] = objective

        // Set the card
        state.card = state.card.copy(
            entries = entries,
            objectives = objectives,
        )
    }

    fun showInventory(player: ServerPlayerEntity) {
        player.openHandledScreen(SimpleNamedScreenHandlerFactory(
            { syncId, inv, _ ->
                CardScreenHandler(
                    syncId = syncId,
                    options = options,
                    state = state,
                    team = teamService.getPlayerTeam(player),
                    card = state.card,
                    playerInventory = inv,
                )
            },
            Text.literal("BINGO Card")
        ))
    }

    private fun generate(options: BingoOptions, seed: Long): BingoCard {
        val dist = options.itemDistribution
        assert(dist.size == TierLabel.entries.size) { "Item distribution does not match the number of tiers (${TierLabel.entries.size})!" }
        assert(dist.sum() <= 25) { "Item distribution exceeds 25!" }

        // Exclude any items from the card that the player might spawn with
        // (e.g. elytra mode, player/team kits...)
        val excludedObjectives = if (configService.config.excludeSpawnKitItemsFromCards) {
            buildSet {
                if (options.isPlayerKit)
                    addAll(options.playerKitItems.map { it.id })
                if (options.isTeamKit)
                    addAll(options.teamKitItems.map { it.id })
                if (options.isElytra) {
                    add("minecraft:elytra")
                    add("minecraft:firework_rocket")
                }
            }.toMutableSet()
        } else mutableSetOf()

        val random = Random(seed)
        val state = CardGeneratorState(random)
        log.info("Using random BINGO card seed: $seed")

        // Collect the selected tier list configs by weight
        val tiers = options.tierLists.entries
            .mapNotNull { (name, weight) ->
                val list = tierListService.tierLists[name]
                    ?.expandTags(tagExpansionService)

                if (list == null) {
                    log.error("Unable to find tier list '$name'!")
                    return@mapNotNull null
                }

                list to weight.coerceAtLeast(1)
            }

        val tiersWeightTotal = tiers.sumOf { it.second }
        assert(tiersWeightTotal > 0) { "There must be at least one tier list selected!" }

        val items = mutableListOf<BingoCardEntry>()
        val objectives = mutableMapOf<String, BingoObjective>()

        // Pick the given number of items from the tier list for each tier
        for ((tierIndex, tier) in TierLabel.entries.withIndex()) {
            var tierCountRemaining = dist[tierIndex]
            var tierWeightRemaining = tiersWeightTotal

            // For each tier list, calculate the number of items it should pick for this tier
            // - shuffled to ensure that list priority is randomized
            for ((list, weight) in tiers.shuffled(random)) {
                val ratio = weight.toDouble() / tierWeightRemaining
                val itemCount = ceil(ratio * tierCountRemaining).toInt()

                // The actual number of items might differ from the expected amount, if the tier list
                // does not have enough entries in it
                val tierItems = list.pick(tier, excludedObjectives, random)
                    .filterIsInstance<TierListEntry.SingleEntry>()
                    .mapNotNull { entry ->
                        val objectiveId = entry.item.substringAfterLast("!")
                        val objectiveMap = createObjectives(objectiveId, state) ?: return@mapNotNull null

                        val objectiveConflicts = objectiveMap.values
                            .flatMap { it.conflictsWithObjectives }
                            .filter { it != objectiveId } // any direct objectiveId conflicts are handled by list.pick()
                        if (objectiveConflicts.any { excludedObjectives.contains(it) })
                            return@mapNotNull null

                        // add this objective and any dependencies to the excluded list to prevent conflicting goals
                        // - this prevents "craft a shield" and "never obtain shield" from appearing on the same card
                        excludedObjectives.addAll(objectiveConflicts)

                        Pair(objectiveId, objectiveMap)
                    }
                    .take(itemCount)
                    .toList()

                for ((objectiveId, goalDependencies) in tierItems) {
                    objectives.putAll(goalDependencies)
                    items.add(
                        BingoCardEntry(
                            objectiveId = objectiveId,
                            tier = tier,
                        )
                    )
                }

                tierCountRemaining -= tierItems.size
                tierWeightRemaining -= weight
            }
        }

        if (items.size < dist.sum()) {
            log.error("Card has generated with ${items.size} entries, when the item distribution needed ${dist.sum()}!")
            log.error("This might be caused by a small tier list or too many item groups in the configuration...")
        }

        // if the card has less than 25 entries, add free spaces until it is full
        val freeSpace = BingoObjective.FreeSpace()
        while (items.size < 25) {
            objectives[freeSpace.id] = freeSpace
            items.add(BingoCardEntry(
                objectiveId = freeSpace.id,
                tier = null,
            ))
        }

        items.shuffle(random)
        return BingoCard(
            entries = items,
            objectives = objectives
        )
    }

    private fun createObjectives(
        objectiveId: String,
        state: CardGeneratorState,
        objectives: MutableMap<String, BingoObjective> = mutableMapOf(),
    ): Map<String, BingoObjective>? {
        val objective = objectiveManager.find(objectiveId, state)
        if (objective == null) {
            log.warn("[CardService] Could not locate objective '$objectiveId'")
            return null
        }

        objectives[objectiveId] = objective

        val isResolved = objective.dependsOnObjectives.all {
            objectives.containsKey(it) || createObjectives(it, state, objectives) != null
        }

        // if any dependencies fail to resolve, return null
        if (!isResolved) {
            log.warn("[CardService] Could not resolve all dependencies for objective '$objectiveId'")
            return null
        }

        return objectives
    }
}