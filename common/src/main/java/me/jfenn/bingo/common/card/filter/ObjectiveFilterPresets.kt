package me.jfenn.bingo.common.card.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.client.platform.text.ITextFactory
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.formatTitle

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
            ?: formatName(text, id)

    companion object {
        fun formatName(textFactory: ITextFactory, name: String): IText =
            textFactory.translatable("bingo.list.$name", name.formatTitle())
    }
}
