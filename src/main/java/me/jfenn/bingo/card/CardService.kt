package me.jfenn.bingo.card

import me.jfenn.bingo.config.BingoConfig
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text

object CardService {

    fun generate(config: BingoConfig): BingoCard {
        val dist = config.itemDistribution
        assert(dist.size == GeneratorTier.values().size ) { "Item distribution does not match the number of tiers (${GeneratorTier.values().size})!" }
        assert(dist.sum() == 25) { "Item distribution does not add up to 25!" }

        // Exclude any items from the card that the player might spawn with
        // (e.g. elytra mode, player/team kits...)
        val excludedItems = if (config.excludeSpawnKitItemsFromCards) {
            buildSet {
                if (config.isPlayerKit)
                    addAll(config.playerKitItems.mapNotNull { it.toIdentifier() })
                if (config.isTeamKit)
                    addAll(config.teamKitItems.mapNotNull { it.toIdentifier() })
                if (config.isElytra) {
                    add(Registries.ITEM.getId(Items.ELYTRA))
                    add(Registries.ITEM.getId(Items.FIREWORK_ROCKET))
                }
            }
        } else emptySet()

        val items = buildList {
            GeneratorTier.values()
                .withIndex()
                .forEach { (index, value) ->
                    addAll(value.pick(dist[index], excludedItems))
                }
        }
            .map { BingoCardEntry(it) }
            .shuffled()

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

}