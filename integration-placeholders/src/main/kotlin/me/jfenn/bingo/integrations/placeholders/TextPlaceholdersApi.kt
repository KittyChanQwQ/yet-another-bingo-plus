package me.jfenn.bingo.integrations.placeholders

import eu.pb4.placeholders.api.*
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.placeholders.api.parsers.TagLikeParser
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.ITextFactory
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import org.apache.commons.lang3.tuple.Pair
import java.util.function.Function
import java.util.function.Supplier

class TextPlaceholdersApi(
    private val text: ITextFactory,
) : ITextPlaceholdersApi {
    override fun registerPlaceholder(id: String, handler: Function<IPlaceholderContext, IText>) {
        Placeholders.registerServer<String>(Identifier.fromNamespaceAndPath("yet-another-minecraft-bingo", id)) { ctx, _ ->
            try {
                handler.apply(BingoPlaceholderContext(ctx))
                    .let { PlaceholderResult.value(it.value) }
            } catch (e: Throwable) {
                PlaceholderResult.invalid("${e::class.simpleName}: ${e.message}")
            }
        }
    }

    override fun parseText(message: IText, server: MinecraftServer): IText {
        return Placeholders.SERVER_PLACEHOLDER_PARSER.parseComponent(
            TextNode.convert(message.value),
            ServerPlaceholderContext.of(server).asParserContext()
        ).let { text.from(it) }
    }

    override fun parseInline(message: IText, server: MinecraftServer, replacements: Map<String, Supplier<IText>>): IText {
        val parser = TagLikeParser.of(
            Pair.of(TagLikeParser.PLACEHOLDER, TagLikeParser.Provider.placeholder(ServerPlaceholderContext.SERVER_KEY, Placeholders.SERVER_PLACEHOLDER_GETTER)),
            Pair.of(TagLikeParser.PLACEHOLDER, TagLikeParser.Provider.placeholder(ServerPlaceholderContext.SERVER_KEY, PlaceholderGetter(replacements))),
        )
        return parser.parseComponent(
            TextNode.convert(message.value),
            ServerPlaceholderContext.of(server).asParserContext(),
        ).let { text.from(it) }
    }
}

class PlaceholderGetter(
    private val placeholders: Map<String, Supplier<IText>>,
) : Placeholders.PlaceholderGetter<ServerPlaceholderContext> {
    override fun getPlaceholder(id: String): Placeholder<ServerPlaceholderContext?, *>? {
        return placeholders[id]
            ?.let {
                Placeholder(
                    Identifier.parse(id),
                    ArgumentParser.NULLABLE_STRING,
                    BingoPlaceholderHandler(it)
                )
            }
    }
}

class BingoPlaceholderHandler(
    private val supplier: Supplier<IText>,
) : Placeholder.Handler<ServerPlaceholderContext, String> {
    override fun onPlaceholderRequest(context: ServerPlaceholderContext?, argument: String?): PlaceholderResult {
        return try {
            PlaceholderResult.value(supplier.get().value)
        } catch (e: Throwable) {
            PlaceholderResult.invalid("${e::class.simpleName}: ${e.message}")
        }
    }
}
