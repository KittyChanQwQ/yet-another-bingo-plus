package me.jfenn.bingo.common.text

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

    fun string(key: String, vararg args: Any): MutableText {
        val fallback = lang[key] ?: run {
            logger.error("[TextProvider] No translation key for $key!")
            key
        }

        return Text.translatableWithFallback(key, fallback, *args)
    }

}