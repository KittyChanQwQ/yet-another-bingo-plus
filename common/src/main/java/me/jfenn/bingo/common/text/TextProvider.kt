package me.jfenn.bingo.common.text

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.client.platform.text.ITextFactory
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.common.utils.jsonUnpretty
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.ITextSerializer
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*

class TextProvider(
    private val logger: Logger,
    private val textSerializer: ITextSerializer,
    private val textFactory: ITextFactory,
): ITextFactory by textFactory {

    private val lang: Map<StringKey, String> = this::class.java.getResourceAsStream("/assets/yet-another-minecraft-bingo/lang/en_us.json")
        ?.use { stream ->
            @OptIn(ExperimentalSerializationApi::class)
            json.decodeFromStream<Map<String, String>>(stream)
        }
        ?.entries
        ?.mapNotNull { (key, value) ->
            val enumKey = StringKey.entries.find { it.key == key }
                ?: return@mapNotNull null

            enumKey to value
        }
        ?.toMap()
        ?: emptyMap()

    fun raw(str: StringKey, vararg args: Any): String {
        val fallback = lang[str] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        return String.format(Locale.getDefault(), fallback, *args)
    }

    fun string(str: StringKey): IText {
        val fallback = lang[str] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        return textFactory.translatable(str.key, fallback)
    }

    fun string(str: StringKey, vararg args: Any): IText {
        val fallback = lang[str] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        val mappedArgs = args
            .map {
                when (it) {
                    is StringKey -> string(it)
                    is IText -> it.value
                    else -> it
                }
            }
            .toTypedArray()

        return textFactory.translatable(str.key, fallback, *mappedArgs)
    }

    fun serialized(text: IText): JsonElement = jsonUnpretty.decodeFromString(textSerializer.toJson(text.value))

    fun deserialized(text: JsonElement) = textFactory.from(textSerializer.fromJson(jsonUnpretty.encodeToString(text)).copy())

    fun boolean(value: Boolean): IText {
        return when (value) {
            true -> string(StringKey.OptionsValueOn).formatted(Formatting.GREEN)
            false -> string(StringKey.OptionsValueOff).formatted(Formatting.RED)
        }
    }

    fun itemCount(number: Int): IText {
        return when (number) {
            1 -> string(StringKey.GameItemSingle)
            else -> string(StringKey.GameItemCount, number)
        }
    }

    fun lineCount(number: Int): IText {
        return when (number) {
            1 -> string(StringKey.GameLineSingle)
            else -> string(StringKey.GameLineCount, number)
        }
    }

    fun cardCount(number: Int): IText {
        return when (number) {
            1 -> string(StringKey.GameCardSingle)
            else -> string(StringKey.GameCardCount, number)
        }
    }

    fun teamCount(number: Int): IText {
        return when (number) {
            1 -> string(StringKey.GameTeamSingle)
            else -> string(StringKey.GameTeamCount, number)
        }
    }

}