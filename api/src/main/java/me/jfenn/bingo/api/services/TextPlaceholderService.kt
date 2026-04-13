package me.jfenn.bingo.api.services

import me.jfenn.bingo.api.annotations.BingoInternalApi
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer

interface TextPlaceholderService {
    fun parseText(message: Component, server: MinecraftServer): Component
}

object TextPlaceholders {
    @BingoInternalApi
    @JvmStatic
    val services = mutableListOf<TextPlaceholderService>()

    @JvmStatic
    @OptIn(BingoInternalApi::class)
    fun register(service: TextPlaceholderService) {
        services.add(service)
    }
}
