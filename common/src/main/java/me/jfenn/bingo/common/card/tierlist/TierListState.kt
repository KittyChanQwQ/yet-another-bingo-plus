package me.jfenn.bingo.common.card.tierlist

import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.utils.formatTitle
import net.minecraft.text.MutableText
import net.minecraft.text.Text

class TierListState {
    var tierLists: Map<String, TierListConfig> = emptyMap()

    fun entries() = tierLists.keys.map { title(it) to it }

    companion object {
        fun title(listName: String): MutableText {
            return Text.translatableWithFallback("$MOD_ID.tierlist.$listName", listName.formatTitle())
        }
    }
}