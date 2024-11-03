package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.card.filter.ObjectiveFilter
import me.jfenn.bingo.common.card.filter.ObjectiveFilterList
import me.jfenn.bingo.common.card.tierlist.TierLabel
import me.jfenn.bingo.common.utils.joinText
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.MutableText
import net.minecraft.text.Text

@Serializable
data class BingoCardOptions(
    var goal: BingoGoal = BingoGoal.Lines(3),
    var itemDistribution: List<Int> = listOf(0, 5, 7, 7, 6),
    var itemFilter: ObjectiveFilterList =
        ConfigService().config.itemFilterPresets[ObjectiveFilter.EVERYTHING]
            ?: ObjectiveFilterList.EVERYTHING,

    // game mode settings
    var isLockoutMode: Boolean = false,
    var isInventoryMode: Boolean = false,
    var isHiddenItemsMode: Boolean = false,
) {
    fun isValid(): Boolean {
        return itemDistribution.sum() <= 25
    }

    fun formatGameMode(): List<StringKey> {
        val gameMode = buildList {
            if (isLockoutMode) add(StringKey.OptionsModeLockout)
            if (isInventoryMode) add(StringKey.OptionsModeInventory)
            if (isHiddenItemsMode) add(StringKey.OptionsModeHiddenItems)
        }

        return gameMode.ifEmpty { listOf(StringKey.OptionsModeStandard) }
    }

    fun formatItemDist(): Text {
        return TierLabel.entries
            .mapIndexed { i, label ->
                Text.literal("${itemDistribution[i]}").formatted(label.formatting)
            }
            .joinText(Text.literal(" "))
    }

    fun formatFilter(config: BingoConfig): MutableText {
        return config.itemFilterPresets.toList()
            .find { (_, filter) -> filter == itemFilter }
            ?.first
            ?.let { name -> ObjectiveFilterList.formatName(name) }
            ?: Text.literal(itemFilter.toString())
    }
}
