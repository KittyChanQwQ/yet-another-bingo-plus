package me.jfenn.bingo.common.card

import kotlinx.serialization.serializer
import me.jfenn.bingo.common.card.filter.ObjectiveFilter
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.objective.BingoObjective
import me.jfenn.bingo.common.card.objective.BingoObjectiveManager
import me.jfenn.bingo.common.card.tag.TagData
import me.jfenn.bingo.common.card.tag.TagService
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.card.tierlist.TierListConfig
import me.jfenn.bingo.common.card.tierlist.TierListEntry
import me.jfenn.bingo.common.card.tierlist.TierListState
import me.jfenn.bingo.common.config.BingoCardOptions
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.Build
import me.jfenn.bingo.common.utils.cacheFor
import me.jfenn.bingo.common.utils.formatString
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.Duration.Companion.seconds

class CardService(
    private val log: Logger,
    private val state: BingoState,
    private val options: BingoOptions,
    private val configService: ConfigService,
    private val tierListState: TierListState,
    private val tagService: TagService,
    private val objectiveManager: BingoObjectiveManager,
    private val tagExpansionService: TagExpansionService,
    private val text: TextProvider,
) {

    private val supportedObjectiveIds = cacheFor(5.seconds) { _: Unit ->
        objectiveManager.list().toSet()
    }

    init {
        validateLists()
        createInitialCards()
    }

    private fun validateLists() {
        tierListState.tierLists.entries
            .filter { (_, list) -> Build.isDebug || list.shouldValidate }
            .forEach { (name, list) -> validateList(name, list) }
    }

    private fun validateList(name: String, list: TierListConfig) {
        log.info("[CardService] Validating list $name.tierlist.json...")
        val state = CardGeneratorState(Random.Default)
        for (entry in list.expandTags(tagExpansionService).let { it.s + it.a + it.b + it.c + it.d }) {
            val map = createObjectives(entry.item, state)
            if (map == null) {
                log.error("[CardService] Could not find '${entry.item}' in tier list $name.tierlist.json")
            }
        }
    }

    fun createInitialCards() {
        if (state.cards.isNotEmpty()) return

        log.info("[CardService] Generating initial cards from config.json")

        val cardOptionsList = options.cards
            .takeIf { it.isNotEmpty() && options.isValid() }
            ?: listOf(BingoCardOptions())

        options.cards = cardOptionsList

        // Note: cardOptionsList is reversed, since newCard adds to the front of the cards
        // (same as /bingo card push)
        for (cardOptions in cardOptionsList.reversed()) {
            try {
                newCard(cardOptions)
            } catch (e: Throwable) {
                log.info("[CardService] Cannot create card from config:", e)
            }
        }
    }

    fun isSupported(err: (Text) -> Unit): Boolean {
        val supportedLists = state.cards
            .flatMap { getTierLists(it.options.itemFilter).keys }
            .distinct()
            .filter { tierList ->
                isListSupported(tierList, err)
            }

        // Each card must have at least one supported list
        return state.cards
            .map { getTierLists(it.options.itemFilter).keys }
            .all { lists -> lists.any { supportedLists.contains(it) } }
    }

    private fun isListSupported(name: String, err: (Text) -> Unit): Boolean {
        // If the list is not found, the game should ignore it
        val list = tierListState.tierLists[name] ?: return true
        val listName by lazy { ObjectiveFilterList.formatName(name).formatted(Formatting.BLUE) }
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

    fun newCard(cardOptions: BingoCardOptions): BingoCard {
        if (!cardOptions.isValid()) throw IllegalArgumentException("Cannot create card; invalid options")

        val newCard = generate(
            id = UUID.randomUUID(),
            seed = Random.nextLong(),
            cardOptions = cardOptions,
        )

        state.pushCard(newCard)
        return newCard
    }

    fun shuffleCard(card: BingoCard) {
        if (!card.options.isValid()) throw IllegalArgumentException("Cannot reroll card; invalid options")

        // Change the BINGO seed to a new value
        val newSeed = Random.nextLong()
        val newCard = generate(card.id, newSeed, card.options)
        state.replaceCard(newCard)
    }

    fun generateCard(
        card: BingoCard = state.getActiveCard(),
        seed: Long = card.seed
    ): BingoCard {
        if (!card.options.isValid()) {
            throw IllegalArgumentException("Cannot generate card; invalid options")
        }

        val newCard = generate(card.id, seed, card.options)
        state.replaceCard(newCard)
        return newCard
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
        val activeCard = state.getActiveCard()

        // Overwrite the entry being replaced
        val entries = activeCard.entries.toMutableList()
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
            activeCard.objectives[key]?.let {
                objectives[key] = it
            }
        }

        objectives[objective.id] = objective

        assignTileNames(entries)
        // Set the card
        val newCard = BingoCard(
            id = activeCard.id,
            seed = activeCard.seed,
            entries = entries,
            objectives = objectives,
            options = activeCard.options
        )
        state.replaceCard(newCard)
    }

    private fun getFilterTag(tags: Map<String, TagData>, filter: ObjectiveFilter): TagData? {
        return tags[filter.tag] ?: run {
            if (!tierListState.tierLists.containsKey(filter.tag) && filter.tag != ObjectiveFilter.UNCATEGORIZED) {
                log.error("[CardService] Objective tag '${filter.tag}' does not exist!")
            }
            null
        }
    }

    private fun getTierLists(
        filterList: ObjectiveFilterList,
    ): Map<String, TierListConfig> {
        // If there are included tier lists, only include the referenced lists
        val hasIncludeList = filterList
            .filterIsInstance<ObjectiveFilter.Include>()
            .any { tierListState.tierLists.containsKey(it.tag) }

        val lists = tierListState.tierLists
            // Only include tier lists that aren't excluded
            .filterNot { (listName, _) ->
                filterList.filterIsInstance<ObjectiveFilter.Exclude>()
                    .any { it.tag == listName }
            }
            .filter { (listName, _) ->
                // If the list is included through either count or include filters, include it
                !hasIncludeList ||
                        filterList.filterIsInstance<ObjectiveFilter.Include>()
                            .any { it.tag == listName } ||
                        filterList.filterIsInstance<ObjectiveFilter.Count>()
                            .any { it.tag == listName }
            }

        return lists
    }

    private fun assembleTierList(
        tags: MutableMap<String, TagData>,
        filterList: ObjectiveFilterList,
    ): TierListConfig {
        // Collect the selected tier list config
        val tierList = run {
            // Otherwise, collect all available lists
            var list = TierListConfig.EMPTY
            getTierLists(filterList)
                .filter { (name, _) -> isListSupported(name, {}) }
                .map { (name, otherList) ->
                    otherList.expandTags(tagExpansionService).expandName(name)
                }
                .forEach { list = list.combine(it) }

            list
        }

        // Determine the set of uncategorized objectives & add them to the tags for filtering
        val uncategorizedObjectives = supportedObjectiveIds.get(Unit)
            .plus(tierList.values.map { it.item }.toSet())
            .filterNot { tierList.isCategorized(it) }
            .map { TierListEntry(it) }
            .onEach {
                it.listName = ObjectiveFilter.UNCATEGORIZED
            }

        tags[ObjectiveFilter.UNCATEGORIZED] = TagData(
            uncategorizedObjectives.map { it.item }.toSet()
        )

        // If there is any included tag, only include explicitly referenced items
        val hasIncludeTag = filterList.any { it is ObjectiveFilter.Include }

        val includeFilters = filterList
            // If a tag is included through either include or count filters, include it
            .filter { it is ObjectiveFilter.Include || it is ObjectiveFilter.Count }
            .mapNotNull { getFilterTag(tags, it) }
            .toMutableList()
        val excludeFilters = filterList
            .filterIsInstance<ObjectiveFilter.Exclude>()
            .mapNotNull { getFilterTag(tags, it) }
            .toMutableList()

        return tierList
            .copy(
                // include any uncategorized objectives in the list
                values = uncategorizedObjectives.toSet()
            )
            .filter { entry ->
                // apply the selected item filters to the list contents
                val isIncluded = !hasIncludeTag || includeFilters.any { it.contains(entry.item) }
                val isExcluded = excludeFilters.any { it.contains(entry.item) }
                isIncluded && !isExcluded
            }
    }

    fun generate(
        id: UUID,
        seed: Long,
        cardOptions: BingoCardOptions,
        excludeObjectives: Set<String> = emptySet(),
    ): BingoCard {
        val dist = cardOptions.itemDistribution
        assert(dist.size == TierLabel.entries.size) { "Item distribution does not match the number of tiers (${TierLabel.entries.size})!" }
        assert(dist.sum() <= 25) { "Item distribution exceeds 25!" }

        // Exclude any items from the card that the player might spawn with
        // (e.g. elytra mode, player/team kits...)
        val excludedObjectives = if (configService.config.excludeSpawnKitItemsFromCards) {
            buildSet {
                addAll(excludeObjectives)

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

        val tags = tagService.getTags().toMutableMap()
        val tierList = assembleTierList(tags, cardOptions.itemFilter)
        val items = mutableListOf<BingoCardEntry>()
        val objectives = mutableMapOf<String, BingoObjective>()

        // Track the tags added to the card to enforce filter requirements
        val requiredTagCounts = cardOptions.itemFilter
            .filterIsInstance<ObjectiveFilter.Count>()
            .mapNotNull { filter ->
                val tag = getFilterTag(tags, filter) ?: return@mapNotNull null
                var remainingCount = filter.count
                // For each tag filter, distribute its items between the item dist
                // (this may not exactly add up to the remainingCount)
                val filterDist = dist.map { distItems ->
                    val count = ceil(filter.count.toDouble() * distItems / 25)
                        .toInt()
                        .coerceAtMost(remainingCount)

                    remainingCount -= count
                    count
                }

                tag to filterDist
            }

        // Pick the given number of items from the tier list for each tier
        for ((tierIndex, tier) in TierLabel.entries.withIndex()) {
            var tierCountRemaining = dist[tierIndex]

            fun createObjectivePair(entry: TierListEntry): Pair<TierListEntry, Map<String, BingoObjective>>? {
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

                return Pair(entry, objectiveMap)
            }

            fun addObjectivePairs(pairs: List<Pair<TierListEntry, Map<String, BingoObjective>>>) {
                for ((entry, objectiveMap) in pairs) {
                    objectives.putAll(objectiveMap)
                    items.add(
                        BingoCardEntry(
                            objectiveId = entry.item,
                            tier = entry.tierLabel,
                            source = entry.listName,
                        )
                    )
                }
                tierCountRemaining -= pairs.size
            }

            // First, attempt to add items that satisfy the required filter counts
            for ((tag, count) in requiredTagCounts.shuffled(random)) {
                val itemCount = count[tierIndex].coerceAtMost(tierCountRemaining)

                val tierItems = tierList.pick(tier, excludedObjectives, random)
                    .filter { tag.contains(it.item) }
                    .mapNotNull { createObjectivePair(it) }
                    .take(itemCount)
                    .toList()

                addObjectivePairs(tierItems)
            }

            // Then, add remaining items that satisfy other filter conditions
            val tierItems = tierList.pick(tier, excludedObjectives, random)
                // exclude required count tags when picking normal items (otherwise the actual counts may differ)
                .filter { entry ->
                    requiredTagCounts.none { (tag, _) -> tag.contains(entry.item) }
                }
                .mapNotNull { createObjectivePair(it) }
                .take(tierCountRemaining)
                .toList()

            addObjectivePairs(tierItems)
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
                source = null,
            ))
        }

        items.shuffle(random)
        assignTileNames(items)

        return BingoCard(
            id = id,
            seed = seed,
            entries = items,
            objectives = objectives,
            options = cardOptions
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