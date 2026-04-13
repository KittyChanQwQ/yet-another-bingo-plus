package me.jfenn.bingo.integrations.placeholders

import eu.pb4.placeholders.api.ServerPlaceholderContext
import net.minecraft.server.MinecraftServer

class BingoPlaceholderContext(
    val context: ServerPlaceholderContext,
) : IPlaceholderContext {
    override val server: MinecraftServer
        get() = context.server()
}