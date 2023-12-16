package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerPlayerCount(position: Vec3d) {
    registerEntity(EntityType.TEXT_DISPLAY, position) { ctx ->
        putString("text", TextInfo.toJsonString(
            TextInfo.empty()
                .append("Player count: ")
                .append(
                    TextInfo.literal("${ctx.server.currentPlayerCount} / ${ctx.server.maxPlayerCount}").formatted(
                        TextColor.GREEN))
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
