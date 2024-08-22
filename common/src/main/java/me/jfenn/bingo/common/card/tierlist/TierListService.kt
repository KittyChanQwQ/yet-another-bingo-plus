package me.jfenn.bingo.common.card.tierlist

import kotlinx.serialization.encodeToString
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.utils.formatTitle
import me.jfenn.bingo.common.utils.joinText
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

    fun formatTierLists(options: BingoOptions) : MutableText {
        val lists = options.tierLists
            .mapNotNull { (name, weight) ->
                val list = tierListState.tierLists[name] ?: return@mapNotNull null
                getName(name, list) to weight.coerceAtLeast(1)
            }
            .toMap()

        val showPercentage = lists.size > 1
        val total = lists.values.sum()

        return lists.entries
            .map { (name, weight) ->
                when {
                    showPercentage -> Text.empty()
                        .append(name)
                        .append(String.format(" (%.0f%%)", weight * 100f / total))
                    else -> name
                }
            }
            .joinText()
    }

    fun entries() = tierListState.tierLists.entries
        .map { (key, list) -> getName(key, list) to list }
}