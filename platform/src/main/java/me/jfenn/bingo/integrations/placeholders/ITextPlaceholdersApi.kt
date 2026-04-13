package me.jfenn.bingo.integrations.placeholders

import me.jfenn.bingo.platform.text.IText
import net.minecraft.server.MinecraftServer
import java.util.function.Function
import java.util.function.Supplier

interface ITextPlaceholdersApi {
    fun registerPlaceholder(id: String, handler: Function<IPlaceholderContext, IText>)
    fun parseText(message: IText, server: MinecraftServer): IText
    fun parseInline(message: IText, server: MinecraftServer, replacements: Map<String, Supplier<IText>>): IText
}

interface IPlaceholderContext {
    val server: MinecraftServer
}
