package me.jfenn.bingo.common.card

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.config.*
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.Logger
import kotlin.random.Random

class CardService(
    private val log: Logger,
    private val state: BingoState,
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val teamService: TeamService,
    private val configService: ConfigService,
    private val tierListService: TierListService,
    private val advancementManager: IAdvancementManager,
    private val itemStackFactory: IItemStackFactory,
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

        val tiers = tierListService.getTierLists(options.tierLists)
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

                when {
                    isAdvancement -> resolveAdvancement(
                        entry.item.removePrefix(TierListConfig.ADVANCEMENT_PREFIX),
                        tier,
                    )
                    else -> resolveItem(entry.item, tier)
                }
            }
            is TierListEntry.MultiEntry -> {
                val innerEntries = entry.items.mapNotNull { createEntry(tier, TierListEntry.SingleEntry(it)) }

                BingoCardEntry.MultiEntry(innerEntries, tier)
            }
        }
    }

    private fun resolveAdvancement(id: String, tier: TierLabel): BingoCardEntry.AdvancementEntry? {
        return try {
            advancementManager.getAdvancement(server, id)!!
            BingoCardEntry.AdvancementEntry(id, tier)
        } catch (e: Throwable) {
            log.debug("Unable to locate advancement '$id'", e)
            log.warn("Unable to locate advancement '$id' from the tier lists ${options.tierLists}, ignoring...")
            null
        }
    }

    private fun resolveItem(id: String, tier: TierLabel): BingoCardEntry.ItemEntry? {
        val isEnabled = try {
            // this might throw if id is not a valid identifier
            itemStackFactory.isEnabledInWorld(id, server)
        } catch (e: Throwable) {
            log.debug("Unable to locate item '$id'", e)
            false
        }

        return if (isEnabled) {
            BingoCardEntry.ItemEntry(id, tier)
        } else {
            log.warn("Unable to locate item '$id' from the tier lists ${options.tierLists}, ignoring...")
            null
        }
    }
}