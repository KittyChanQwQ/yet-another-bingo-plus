package me.jfenn.bingo.common.card

import kotlinx.serialization.serializer
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.common.card.tierlist.TierListEntry
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.Build
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.reflect.full.declaredMemberProperties

class CardService(
    private val log: Logger,
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val tierListService: TierListState,
    private val objectiveManager: BingoObjectiveManager,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) {
    init {
        validateLists()
    }

    private fun validateLists() {
        tierListService.tierLists.entries
            .filter { (_, list) -> Build.isDebug || list.shouldValidate }
            .forEach { (name, list) -> validateList(name, list) }
    }

    private fun validateList(name: String, list: TierListConfig) {
        log.info("[CardService] Validating list $name.tierlist.json...")
        val state = CardGeneratorState(Random.Default)
        for (entry in list.expandTags(tagExpansionService).let { it.s + it.a + it.b + it.c + it.d }) {
            if (entry is TierListEntry.SingleEntry) {
                val map = createObjectives(entry.item, state)
                if (map == null) {
                    log.error("[CardService] Could not find '${entry.item}' in tier list $name.tierlist.json")
                }
            } else {
                log.error("[CardService] Tier list $name.tierlist.json contains unsupported group entries!")
                log.error("  If this is a built-in list, deleting it from your config folder will allow the mod to recreate it.")
                log.error("  If this is a custom list you've created, you'll need to convert it to a datapack to use 'one_of' objectives: https://minecraft.horrific.dev/bingo/custom-objectives")
            }
        }
    }

    fun isSupported(err: (Text) -> Unit): Boolean =
        options.tierLists.keys.all { isListSupported(it, err) }

    private fun isListSupported(name: String, err: (Text) -> Unit): Boolean {
        val list = tierListService.tierLists[name] ?: return true
        val listName by lazy { TierListState.title(name).formatted(Formatting.BLUE) }
        val reqs by list::requirements

        var isSupported = true

        val teams = state.getRegisteredTeams()
        if (teams.size !in reqs.numTeams) {
            err(text.string(StringKey.CommandStartTierListRecommendsNumTeams, listName, reqs.numTeams.formatString()))
            isSupported = false
        }

        if (teams.any { it.players.size !in reqs.numPlayersPerTeam }) {
            err(text.string(StringKey.CommandStartTierListRecommendsPlayersPerTeam, listName, reqs.numPlayersPerTeam.formatString()))
            isSupported = false
        }

        val missingObjective = reqs.objectives.firstOrNull {
            objectiveManager.find(it, CardGeneratorState(Random.Default)) == null
        }
        if (missingObjective != null) {
            err(text.string(StringKey.CommandStartTierListMissingObjective, listName, missingObjective))
            isSupported = false
        }

        // TODO: Validate reqs.onlyDefaultTeams if more teams are added

        for (prop in BingoOptions::class.declaredMemberProperties) {
            val reqProp = reqs.options[prop.name] ?: continue
            val propSerializer = serializer(prop.returnType)

            val propValue = prop.get(options)
            val reqPropValue = json.decodeFromJsonElement(propSerializer, reqProp)

            if (propValue != reqPropValue) {
                err(text.string(StringKey.CommandStartTierListRecommends, listName, "${prop.name}=$reqPropValue"))
                isSupported = false
            }
        }

        return isSupported
    }

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
            source = null,
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

        assignTileNames(entries)
        // Set the card
        state.card = BingoCard(
            id = state.card.id,
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

                Triple(name, list, weight.coerceAtLeast(1))
            }

        val tiersWeightTotal = tiers.sumOf { it.third }
        assert(tiersWeightTotal > 0) { "There must be at least one tier list selected!" }

        val items = mutableListOf<BingoCardEntry>()
        val objectives = mutableMapOf<String, BingoObjective>()
        var missingItems = 0

        // Pick the given number of items from the tier list for each tier
        for ((tierIndex, tier) in TierLabel.entries.withIndex()) {
            var tierCountRemaining = dist[tierIndex]
            var tierWeightRemaining = tiersWeightTotal

            // - shuffled to ensure that list priority is randomized on each tier
            val tierLists = tiers.shuffled(random)

            fun createObjectivePair(entry: TierListEntry.SingleEntry): Pair<String, Map<String, BingoObjective>>? {
                val objectiveId = entry.item
                val objectiveMap = createObjectives(objectiveId, state)
                    ?: return null

                val objectiveConflicts = objectiveMap.values
                    .flatMap { it.conflictsWithObjectives }
                    .filter { it != objectiveId } // any direct objectiveId conflicts are handled by list.pick()
                if (objectiveConflicts.any { excludedObjectives.contains(it) })
                    return null

                // add this objective and any dependencies to the excluded list to prevent conflicting goals
                // - this prevents "craft a shield" and "never obtain shield" from appearing on the same card
                excludedObjectives.addAll(objectiveConflicts)

                return Pair(objectiveId, objectiveMap)
            }

            // For each tier list, calculate the number of items it should pick for this tier
            for ((listId, list, weight) in tierLists) {
                val ratio = weight.toDouble() / tierWeightRemaining
                val itemCount = ceil(ratio * tierCountRemaining).toInt()

                // The actual number of items might differ from the expected amount, if the tier list
                // does not have enough entries in it
                val tierItems = list.pick(tier, excludedObjectives, random)
                    .filterIsInstance<TierListEntry.SingleEntry>()
                    .mapNotNull { createObjectivePair(it) }
                    .take(itemCount)
                    .toList()

                for ((objectiveId, objectiveMap) in tierItems) {
                    objectives.putAll(objectiveMap)
                    items.add(
                        BingoCardEntry(
                            objectiveId = objectiveId,
                            tier = tier,
                            source = listId,
                        )
                    )
                }

                tierCountRemaining -= tierItems.size
                tierWeightRemaining -= weight
            }

            // If the tier's items don't add up to the expected count, try to fill the remainder from other lists
            for (i in 0 until tierCountRemaining) {

                // Find the first item that can be obtained from any tier list
                for ((listId, list, _) in tierLists) {
                    val (objectiveId, objectiveMap) = list.pick(tier, excludedObjectives, random)
                        .filterIsInstance<TierListEntry.SingleEntry>()
                        .mapNotNull { createObjectivePair(it) }
                        .firstOrNull()
                        ?: continue

                    objectives.putAll(objectiveMap)
                    items.add(
                        BingoCardEntry(
                            objectiveId = objectiveId,
                            tier = tier,
                            source = listId,
                        )
                    )
                    missingItems++
                    break
                }
            }
        }

        if (items.size < dist.sum() || missingItems > 0) {
            log.error("Card has generated with ${items.size - missingItems} entries, when the item distribution needed ${dist.sum()}!")
            log.error("This might be caused by a small tier list or too many item groups in the configuration...")
        }

        // if the card has less than 25 entries, add free spaces until it is full
        val freeSpace = BingoObjective.FreeSpace()
        while (items.size < 25) {
            objectives[freeSpace.id] = freeSpace
            items.add(BingoCardEntry(
                objectiveId = freeSpace.id,
                tier = null,
                source = null,
            ))
        }

        items.shuffle(random)
        assignTileNames(items)

        return BingoCard(
            entries = items,
            objectives = objectives
        )
    }

    private fun assignTileNames(entries: List<BingoCardEntry>) {
        val cols = "bingo".toCharArray()
        val rows = "12345".toCharArray()
        entries.forEachIndexed { i, entry ->
            val x = i % 5
            val y = i / 5
            entry.tileName = "${cols[x]}${rows[y]}"
        }
    }

    private fun createObjectives(
        objectiveId: String,
        state: CardGeneratorState,
        objectives: MutableMap<String, BingoObjective> = mutableMapOf(),
    ): Map<String, BingoObjective>? {
        val objective = objectiveManager.find(objectiveId, state) ?: return null

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