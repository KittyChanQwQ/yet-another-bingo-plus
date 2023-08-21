package me.jfenn.bingo.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting

fun PlayerEntity.allHeldStacks() = sequence<ItemStack> {
    // yield all items in inventory
    for (i in 0 until inventory.size()) {
        yield(inventory.getStack(i))
    }

    // yield the current cursor stack
    yield(currentScreenHandler.cursorStack)
}

fun PlayerEntity.sendInfoMessage(text: String) {
    sendMessage(
        Text.empty()
            .append(Text.literal("ℹ  ").formatted(Formatting.AQUA))
            .append(Text.literal(text).formatted(Formatting.AQUA, Formatting.ITALIC)),
        false,
    )
}

fun PlayerEntity.sendWarningMessage(text: String) {
    sendMessage(
        Text.empty()
            .append(Text.literal("⚠  ").formatted(Formatting.YELLOW))
            .append(Text.literal(text).formatted(Formatting.YELLOW, Formatting.ITALIC)),
        false,
    )
}
