package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.utils.text.TextColor
import me.jfenn.bingo.common.utils.text.TextInfo
import net.minecraft.entity.EntityType
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerPlayerCount(
    position: Vec3d,
    server: MinecraftServer = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", TextInfo.toJsonString(
            TextInfo.empty()
                .append("Player count: ")
                .append(
                    TextInfo.literal("${server.currentPlayerCount} / ${server.maxPlayerCount}").formatted(
                        TextColor.GREEN))
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
