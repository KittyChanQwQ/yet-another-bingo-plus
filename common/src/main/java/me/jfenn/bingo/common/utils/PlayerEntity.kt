package me.jfenn.bingo.common.utils

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting

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

fun PlayerEntity.hasPermission(level: Int = 2): Boolean {
    return hasPermissionLevel(level) || server?.isSingleplayer == true
}
