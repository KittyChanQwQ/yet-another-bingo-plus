package me.jfenn.bingo.common.card

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListEntry
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.json
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
        val excludedItems = if (configService.config.excludeSpawnKitItemsFromCards) {
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
                val tierItems = list.pick(tier, excludedItems, random)
                    .filterIsInstance<TierListEntry.SingleEntry>()
                    .mapNotNull { entry ->
                        val objectiveId = entry.item.substringAfterLast("!")
                        createGoal(objectiveId, state)?.let { Pair(objectiveId, it) }
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
        return BingoCard(items, objectives)
    }

    private fun createGoal(
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
            objectives.containsKey(it) || createGoal(it, state, objectives) != null
        }

        // if any goal dependencies fail to resolve, return null
        if (!isResolved) {
            log.warn("[CardService] Could not resolve all dependencies for objective '$objectiveId'")
            return null
        }

        return objectives
    }
}