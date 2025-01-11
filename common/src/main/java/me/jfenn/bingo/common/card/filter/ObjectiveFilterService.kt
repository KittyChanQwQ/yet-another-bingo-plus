package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.data.ScopedData
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class ObjectiveFilterService(
    private val config: BingoConfig,
    private val data: ScopedData,
) {
    fun getPresetFilters(): Map<String, ObjectiveFilterList> {
        return config.itemFilterPresets + data.filterPresets.filters
    }

    fun formatFilter(itemFilter: ObjectiveFilterList, includePresetDetails: Boolean): MutableText {
        return getPresetFilters().toList()
            .find { (_, filter) -> filter == itemFilter }
            ?.let { (name, filter) ->
                val text = ObjectiveFilterList.formatName(name)
                when {
                    includePresetDetails -> text.append(" ")
                        .append(Text.literal("($filter)").formatted(Formatting.GRAY))
                    else -> text
                }
            }
            ?: Text.literal(itemFilter.toString())
    }
}