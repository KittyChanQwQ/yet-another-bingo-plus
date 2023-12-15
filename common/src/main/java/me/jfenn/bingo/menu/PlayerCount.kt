package me.jfenn.bingo.menu

import net.minecraft.entity.EntityType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerPlayerCount(position: Vec3d) {
    registerEntity(EntityType.TEXT_DISPLAY, position) { ctx ->
        putString("text", Text.Serialization.toJsonString(
            Text.empty()
                .append("Player count: ")
                .append(Text.literal("${ctx.server.currentPlayerCount} / ${ctx.server.maxPlayerCount}").formatted(Formatting.GREEN))
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
