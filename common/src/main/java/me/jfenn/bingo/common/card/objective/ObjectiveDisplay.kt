package me.jfenn.bingo.common.card.objective

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.item.IItemStack
import net.minecraft.text.Text

class ObjectiveDisplay {
    data class Resolved(
        val name: Text? = null,
        val lore: List<Text> = emptyList(),
        val item: IItemStack? = null,
    ) {
        companion object {
            val EMPTY = Resolved()
        }
    }

    @Serializable
    class Data(
        val name: String? = null,
        val lore: List<String> = emptyList(),
        val item: String? = null,
        val itemNbt: String? = null,
        val itemComponents: Map<String, String>? = null,
    )
}