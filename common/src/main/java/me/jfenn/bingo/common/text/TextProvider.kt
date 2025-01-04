package me.jfenn.bingo.common.text

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.ITextSerializer
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*

class TextProvider(
    private val logger: Logger,
    private val textSerializer: ITextSerializer,
) {

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

    fun string(str: StringKey): MutableText {
        val fallback = lang[str] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        return Text.translatableWithFallback(str.key, fallback)
    }

    fun string(str: StringKey, vararg args: Any): MutableText {
        val fallback = lang[str] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        val mappedArgs = args
            .map {
                when {
                    it is StringKey -> string(it)
                    else -> it
                }
            }
            .toTypedArray()

        return Text.translatableWithFallback(str.key, fallback, *mappedArgs)
    }

    fun serialized(text: Text) = textSerializer.toJson(text)

    fun boolean(value: Boolean): MutableText {
        return when (value) {
            true -> string(StringKey.OptionsValueOn).formatted(Formatting.GREEN)
            false -> string(StringKey.OptionsValueOff).formatted(Formatting.RED)
        }
    }

    fun itemCount(number: Int): MutableText {
        return when (number) {
            1 -> string(StringKey.GameItemSingle)
            else -> string(StringKey.GameItemCount, number)
        }
    }

    fun lineCount(number: Int): MutableText {
        return when (number) {
            1 -> string(StringKey.GameLineSingle)
            else -> string(StringKey.GameLineCount, number)
        }
    }

    fun cardCount(number: Int): MutableText {
        return when (number) {
            1 -> string(StringKey.GameCardSingle)
            else -> string(StringKey.GameCardCount, number)
        }
    }

}