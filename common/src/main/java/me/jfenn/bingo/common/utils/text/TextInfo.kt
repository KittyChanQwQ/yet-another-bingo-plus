package me.jfenn.bingo.common.utils.text

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TextInfo(
    var text: String,
    var color: TextColor? = null,
    var bold: Boolean? = null,
    var italic: Boolean? = null,
    var underlined: Boolean? = null,
    var strikethrough: Boolean? = null,
    var obfuscated: Boolean? = null,
    var extra: MutableList<TextInfo>? = null,
) {


    fun append(text: String) = append(literal(text))

    fun append(text: TextInfo) : TextInfo {
        extra?.add(text)
            ?: run {
                extra = mutableListOf(text)
            }
        return this
    }

    fun formatted(color: TextColor) : TextInfo {
        this.color = color
        return this
    }

    fun bold() : TextInfo {
        bold = true
        return this
    }

    companion object {
        fun empty() : TextInfo {
            return TextInfo("")
        }

        fun literal(text: String) : TextInfo {
            return TextInfo(text)
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun toJsonString(text: TextInfo) : String {
            val json = Json {
                encodeDefaults = true
                explicitNulls = false
            }

            return json.encodeToString(text)
        }
    }
}
