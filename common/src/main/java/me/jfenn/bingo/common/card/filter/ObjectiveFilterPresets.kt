package me.jfenn.bingo.common.card.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.text.MutableText
import net.minecraft.text.Text

@Serializable
class ObjectiveFilterPresets(
    val filters: List<ObjectiveFilterPreset>
) {
    companion object {
        val EMPTY = ObjectiveFilterPresets(emptyList())
    }
}

@Serializable
class ObjectiveFilterPreset(
    val id: String,
    val name: JsonElement?,
    val value: ObjectiveFilterList,
) {
    fun name(text: TextProvider) =
        name?.let { text.deserialized(it) }
            ?: formatName(id)

    companion object {
        fun formatName(name: String): MutableText = Text.translatableWithFallback("bingo.list.$name", name.formatTitle())
    }
}
