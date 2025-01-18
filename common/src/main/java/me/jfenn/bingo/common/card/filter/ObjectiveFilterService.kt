package me.jfenn.bingo.common.card.filter

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.text.TextProvider
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class ObjectiveFilterService(
    private val config: BingoConfig,
    private val data: ScopedData,
    private val text: TextProvider,
) {
    fun getPresetFilters(): Map<String, ObjectiveFilterPreset> {
        val configPresets = config.itemFilterPresets
            .map { (id, value) -> ObjectiveFilterPreset(id, null, value) }
            .associateBy { it.id }

        val dataPresets = data.filterPresets.filters
            .associateBy { it.id }

        return configPresets + dataPresets
    }

    fun formatFilter(itemFilter: ObjectiveFilterList, includePresetDetails: Boolean): MutableText {
        return getPresetFilters().values
            .find { filter -> filter.value == itemFilter }
            ?.let { filter ->
                val text = filter.name(text)
                when {
                    includePresetDetails -> text.append(" ")
                        .append(Text.literal("($filter)").formatted(Formatting.GRAY))
                    else -> text
                }
            }
            ?: Text.literal(itemFilter.toString())
    }
}