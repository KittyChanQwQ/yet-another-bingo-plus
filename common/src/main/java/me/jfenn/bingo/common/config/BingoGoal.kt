package me.jfenn.bingo.common.config

import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.MutableText
import net.minecraft.text.Text

@Serializable
sealed class BingoGoal {
    @Serializable
    class Items(val items: Int): BingoGoal()
    @Serializable
    class Lines(val lines: Int): BingoGoal()

    companion object {
        const val MAX_ITEMS = 25
        const val MAX_LINES = 11
    }

    fun string(): StringKey {
        return when (this) {
            is Items -> StringKey.GoalItems
            is Lines -> StringKey.GoalLines
        }
    }

    fun isFullCard(): Boolean {
        return when (this) {
            is Items -> items >= MAX_ITEMS
            is Lines -> lines >= MAX_LINES
        }
    }

    fun format(textProvider: TextProvider): MutableText {
        return when {
            this.isFullCard() -> textProvider.string(StringKey.GoalFullCard)
            else -> when (this) {
                is Items -> textProvider.itemCount(this.items)
                is Lines -> textProvider.lineCount(this.lines)
            }
        }
    }
}
