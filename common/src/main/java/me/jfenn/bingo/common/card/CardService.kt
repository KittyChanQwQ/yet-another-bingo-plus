package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.config.*
import me.jfenn.bingo.common.state.BingoState
import net.minecraft.registry.Registries
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import kotlin.random.Random

class CardService(
    private val state: BingoState,
    private val options: BingoOptions,
    private val teamService: TeamService,
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

    companion object {

        private val log = LoggerFactory.getLogger(MOD_ID)

        fun generate(options: BingoOptions, seed: Long): BingoCard {
            val dist = options.itemDistribution
            assert(dist.size == TierList.entries.size) { "Item distribution does not match the number of tiers (${TierList.entries.size})!" }
            assert(dist.sum() <= 25) { "Item distribution exceeds 25!" }

            // Exclude any items from the card that the player might spawn with
            // (e.g. elytra mode, player/team kits...)
            val excludedItems = if (ConfigService.config.excludeSpawnKitItemsFromCards) {
                buildSet {
                    if (options.isPlayerKit)
                        addAll(options.playerKitItems.map { it.id })
                    if (options.isTeamKit)
                        addAll(options.teamKitItems.map { it.id })
                    if (options.isElytra) {
                        add("minecraft:elytra")
                        add("minecraft:firework_rocket")
                    }
                }
            } else emptySet()

            val random = Random(seed)
            log.info("Using random BINGO card seed: $seed")

            val tiers = TierListService.getTierList(options.tierList)
                ?: TierListConfig.EMPTY

            val items: MutableList<BingoCardEntry> = tiers.pick(dist, excludedItems, random)
                .mapNotNull { (entry, tier) -> createEntry(tier, entry) }
                .toMutableList()

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

        private fun createEntry(tier: TierList, entry: TierListEntry): BingoCardEntry? {
            return when (entry) {
                is TierListEntry.SingleEntry -> {
                    val identifier = parseIdentifier(entry.item)
                        ?: return null

                    BingoCardEntry.ItemEntry(identifier, tier)
                }
                is TierListEntry.MultiEntry -> {
                    val innerEntries = entry.items.mapNotNull { createEntry(tier, TierListEntry.SingleEntry(it)) }

                    BingoCardEntry.MultiEntry(innerEntries, tier)
                }
            }
        }

        private fun parseIdentifier(str: String): String? {
            val name = str.trim()
                .lowercase()
                .let {
                    if (it.contains(":"))
                        it
                    else "minecraft:$it"
                }

            val identifier = Identifier.tryParse(name) ?: run {
                log.error("Could not parse item identifier '${name}'")
                return null
            }

            if (!Registries.ITEM.containsId(identifier)) {
                log.error("Item identifier '${name}' does not exist")
                return null
            }

            return identifier.toString()
        }
    }

}