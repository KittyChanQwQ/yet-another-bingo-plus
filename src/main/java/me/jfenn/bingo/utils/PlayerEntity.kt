package me.jfenn.bingo.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

fun PlayerEntity.allHeldStacks() = sequence<ItemStack> {
    // yield all items in inventory
    for (i in 0 until inventory.size()) {
        yield(inventory.getStack(i))
    }

    // yield the current cursor stack
    yield(currentScreenHandler.cursorStack)
}
