package me.jfenn.bingo.common.card

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.TierList
import me.jfenn.bingo.common.config.TierListService
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.Registries
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object CardService {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun generate(config: BingoConfig): BingoCard {
        val dist = config.itemDistribution
        assert(dist.size == TierList.entries.size) { "Item distribution does not match the number of tiers (${TierList.entries.size})!" }
        assert(dist.sum() <= 25) { "Item distribution exceeds 25!" }

        // Exclude any items from the card that the player might spawn with
        // (e.g. elytra mode, player/team kits...)
        val excludedItems = if (config.excludeSpawnKitItemsFromCards) {
            buildSet {
                if (config.isPlayerKit)
                    addAll(config.playerKitItems.map { it.id })
                if (config.isTeamKit)
                    addAll(config.teamKitItems.map { it.id })
                if (config.isElytra) {
                    add("minecraft:elytra")
                    add("minecraft:firework_rocket")
                }
            }
        } else emptySet()

        val tiers = TierListService.getTierLists(config.tierLists)
        val items: MutableList<BingoCardEntry> = tiers.pick(dist, excludedItems)
            .mapNotNull { parseIdentifier(it) }
            .map { BingoCardEntry.ItemEntry(it) }
            .toMutableList()

        if (items.size < dist.sum()) {
            log.error("Card has generated with ${items.size} entries, when the item distribution needed ${dist.sum()}!")
            log.error("This might be caused by a small tier list or too many item groups in the configuration...")
        }

        // if the card has less than 25 entries, add free spaces until it is full
        while (items.size < 25)
            items.add(BingoCardEntry.FreeSpace)

        items.shuffle()
        return BingoCard(items)
    }

    fun showInventory(player: PlayerEntity, card: BingoCard) {
        player.openHandledScreen(SimpleNamedScreenHandlerFactory(
            { syncId, inv, _ ->
                CardScreenHandler(syncId, card, inv)
            },
            Text.literal("BINGO Card")
        ))
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