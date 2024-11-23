package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_KEEP
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.ready.ReadyTimerState
import me.jfenn.bingo.common.scope.BingoKoin
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

object PlayerEntityMixinHandler {

    fun shouldVanishDrop(player: ServerPlayerEntity, stack: ItemStack): Boolean {
        val scope = BingoKoin.getScope(player.server) ?: return false
        val itemStackFactory = scope.get<IItemStackFactory>()
        val itemStack = itemStackFactory.forStack(stack)
        return itemStack.hasCustomTag(NBT_BINGO_VANISH)
    }

    fun shouldKeepDrop(player: ServerPlayerEntity, stack: ItemStack): Boolean {
        val scope = BingoKoin.getScope(player.server) ?: return false
        val itemStackFactory = scope.get<IItemStackFactory>()
        val itemStack = itemStackFactory.forStack(stack)
        return itemStack.hasCustomTag(NBT_BINGO_KEEP)
    }

    fun getPlayerListName(server: MinecraftServer, uuid: UUID, name: Text): Text? {
        val scope = BingoKoin.getScope(server) ?: return null
        val readyTimerState = scope.get<ReadyTimerState>()

        return if (readyTimerState.isReady(uuid)) {
            Text.empty()
                .append(Text.literal("✔ ").formatted(Formatting.GREEN))
                .append(name)
        } else {
            null
        }
    }

}