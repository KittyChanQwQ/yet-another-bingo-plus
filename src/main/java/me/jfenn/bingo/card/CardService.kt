package me.jfenn.bingo.card

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.text.Text

object CardService {

    fun generate(dist: List<Int>): BingoCard {
        assert(dist.size == GeneratorTier.values().size ) { "Item distribution does not match the number of tiers (${GeneratorTier.values().size})!" }
        assert(dist.sum() == 25) { "Item distribution does not add up to 25!" }

        val items = buildList {
            GeneratorTier.values()
                .withIndex()
                .forEach { (index, value) ->
                    addAll(value.pick(dist[index]))
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