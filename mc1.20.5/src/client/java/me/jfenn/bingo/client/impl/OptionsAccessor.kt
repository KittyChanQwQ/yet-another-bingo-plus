package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.api.IOptionsAccessor
import net.minecraft.client.MinecraftClient

object OptionsAccessor : IOptionsAccessor {
    private val client = MinecraftClient.getInstance()

    override fun isDebugEnabled(): Boolean {
        return client.inGameHud.debugHud.shouldShowDebugHud()
    }

    override fun isPlayerListPressed(): Boolean {
        return client.options.playerListKey.isPressed
    }

    override fun isHudHidden(): Boolean {
        return client.options.hudHidden
    }
}
