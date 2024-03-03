package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.config.*
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.Logger
import kotlin.random.Random

class CardService(
    private val log: Logger,
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val tierListService: TierListService,
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
        log.info("Using random BINGO card seed: $seed")

        val tiers = tierListService.tierLists[options.tierList]
            ?: TierListConfig.EMPTY

        val items = mutableListOf<BingoCardEntry>()

        // Pick the given number of items from the tier list for each tier
        for ((tierIndex, tier) in TierLabel.entries.withIndex()) {
            val tierCount = dist[tierIndex]

            tiers.pick(tier, excludedItems, random)
                .mapNotNull { entry -> createEntry(tier, entry) }
                .take(tierCount)
                .forEach { items.add(it) }
        }

        if (items.size < dist.sum()) {
            log.error("Card has generated with ${items.size} entries, when the item distribution needed ${dist.sum()}!")
            log.error("This might be caused by a small tier list or too many item groups in the configuration...")
        }

        // if the card has less than 25 entries, add free spaces until it is full
        while (items.size < 25)
            items.add(BingoCardEntry.FreeSpace())

        items.shuffle(random)
        return BingoCard(items)
    }

    private fun createEntry(tier: TierLabel, entry: TierListEntry): BingoCardEntry? {
        return when (entry) {
            is TierListEntry.SingleEntry -> {
                val isAdvancement = entry.item.startsWith(TierListConfig.ADVANCEMENT_PREFIX)

                try {
                    when {
                        isAdvancement -> BingoCardEntry.AdvancementEntry(
                            entry.item.removePrefix(TierListConfig.ADVANCEMENT_PREFIX),
                            tier,
                        )
                        else -> BingoCardEntry.ItemEntry(entry.item, tier)
                    }
                } catch (e: IllegalArgumentException) {
                    log.error(e.message)
                    null
                }
            }
            is TierListEntry.MultiEntry -> {
                val innerEntries = entry.items.mapNotNull { createEntry(tier, TierListEntry.SingleEntry(it)) }

                BingoCardEntry.MultiEntry(innerEntries, tier)
            }
        }
    }

}