package me.jfenn.bingo.generator

import me.jfenn.bingo.data.BingoCard
import me.jfenn.bingo.data.BingoCardEntry
import me.jfenn.bingo.minecraftServer
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.network.PacketByteBuf
import net.minecraft.registry.Registries
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.*

object CardService {

    val tiers = mapOf<String, GeneratorTier>(
        "A" to GeneratorTier.fromFilename("a_tier.txt"),
        "B" to GeneratorTier.fromFilename("b_tier.txt"),
        "C" to GeneratorTier.fromFilename("c_tier.txt"),
        "D" to GeneratorTier.fromFilename("d_tier.txt"),
        "S" to GeneratorTier.fromFilename("s_tier.txt"),
    )

    fun generate(dist: List<Int>): BingoCard {
        assert(dist.size == tiers.size ) { "Item distribution does not match the number of tiers (${tiers.size})!" }
        assert(dist.sum() == 25) { "Item distribution does not add up to 25!" }

        val items = buildList<String> {
            tiers.iterator()
                .withIndex()
                .forEach { (index, value) ->
                    addAll(value.value.pick(dist[index]))
                }
        }
            .map { BingoCardEntry(it) }
            .shuffled()

        return BingoCard(items)
    }

    fun showInventory(player: PlayerEntity, card: BingoCard) {
        player.openHandledScreen(SimpleNamedScreenHandlerFactory(
            { syncId, _, _ ->
                CardScreenHandler(syncId, card)
            },
            Text.literal("BINGO Card")
        ))
    }

}