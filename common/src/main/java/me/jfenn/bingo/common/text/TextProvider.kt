package me.jfenn.bingo.common.text

import StringKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.common.utils.json
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import org.slf4j.Logger

class TextProvider(
    private val logger: Logger,
) {

    @OptIn(ExperimentalSerializationApi::class)
    val lang: Map<String, String> = this::class.java.getResourceAsStream("/yet-another-minecraft-bingo/lang/en_us.json")
        ?.use { stream ->
            json.decodeFromStream<Map<String, String>>(stream)
        }
        ?: emptyMap()

    fun string(str: StringKey, vararg args: Any): MutableText {
        val fallback = lang[str.key] ?: run {
            logger.error("[TextProvider] No translation key for ${str.key}!")
            str.key
        }

        return Text.translatableWithFallback(str.key, fallback, *args)
    }

    fun itemCount(number: Int): Text {
        return when (number) {
            1 -> string(StringKey.GameItemSingle)
            else -> string(StringKey.GameItemCount, number)
        }
    }

    fun lineCount(number: Int): Text {
        return when (number) {
            1 -> string(StringKey.GameLineSingle)
            else -> string(StringKey.GameLineCount, number)
        }
    }

}