package me.jfenn.bingo.common.card.objective

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.common.map.CardTile
import net.minecraft.text.Text

class ObjectiveDisplay {
    data class Resolved(
        val name: Text? = null,
        val lore: List<Text>? = null,
        val item: IItemStack? = null,
        val decoration: CardTile.Decoration = CardTile.Decoration.NONE,
    ) {
        companion object {
            val EMPTY = Resolved()
        }
    }

    @Serializable
    class Data(
        val name: JsonElement? = null,
        val lore: List<JsonElement>? = null,
        val item: String? = null,
        val itemNbt: String? = null,
        val itemComponents: Map<String, JsonElement>? = null,
        val decoration: CardTile.Decoration? = null,
    )
}