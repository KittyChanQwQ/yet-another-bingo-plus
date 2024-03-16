package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.entity.EntityType
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Vec3d

fun MenuComponent.registerPlayerCount(
    position: Vec3d,
    server: MinecraftServer = koinScope.get(),
    text: TextProvider = koinScope.get(),
) {
    registerEntity(EntityType.TEXT_DISPLAY, position) {
        putString("text", text.serialized(
            text.string(
                StringKey.OptionsPlayerCount,
                Text.literal("${server.currentPlayerCount} / ${server.maxPlayerCount}")
                    .formatted(Formatting.GREEN)
            )
        ))
        putString("billboard", "fixed")
        putString("alignment", "center")
        putInt("background", 0)
        putBoolean("shadow", true)
    }
}
