package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.config.BingoCardOptions
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.jsonUnpretty
import net.minecraft.text.MutableText
import net.minecraft.text.Text

class TierListService(
    private val tierListState: TierListState,
    private val textSerializer: ITextSerializer,
) {
    fun getName(key: String, list: TierListConfig? = tierListState.tierLists[key]): MutableText {
        val listName = list?.name
            ?.let { jsonUnpretty.encodeToString(it) }
            ?.let { textSerializer.fromJson(it) }
            ?.copy()

        return listName ?: Text.translatableWithFallback("$MOD_ID.tierlist.$key", key.formatTitle())
    }

    fun formatTierList(options: BingoCardOptions) : MutableText? {
        return options.tierList?.let { name ->
            val list = tierListState.tierLists[name] ?: return@let null
            getName(name, list)
        }
    }

    fun entries() = tierListState.tierLists.entries
        .map { (key, list) -> getName(key, list) to list }
}