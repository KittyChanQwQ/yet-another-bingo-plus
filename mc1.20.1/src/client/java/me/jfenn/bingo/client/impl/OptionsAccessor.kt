package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.api.IOptionsAccessor
import net.minecraft.client.MinecraftClient

object OptionsAccessor : IOptionsAccessor {
    override fun isDebugEnabled(): Boolean {
        val client = MinecraftClient.getInstance()
        return client.options.debugEnabled
    }

    override fun isPlayerListPressed(): Boolean {
        val client = MinecraftClient.getInstance()
        return client.options.playerListKey.isPressed
    }
}
