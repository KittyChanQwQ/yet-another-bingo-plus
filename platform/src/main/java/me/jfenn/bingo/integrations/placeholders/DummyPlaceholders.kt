package me.jfenn.bingo.integrations.placeholders

import me.jfenn.bingo.platform.text.IText
import net.minecraft.server.MinecraftServer
import java.util.function.Function
import java.util.function.Supplier

object DummyPlaceholders : ITextPlaceholdersApi {
    override fun registerPlaceholder(id: String, handler: Function<IPlaceholderContext, IText>) {}

    override fun parseText(message: IText, server: MinecraftServer): IText = message

    override fun parseInline(message: IText, server: MinecraftServer, replacements: Map<String, Supplier<IText>>): IText = message
}