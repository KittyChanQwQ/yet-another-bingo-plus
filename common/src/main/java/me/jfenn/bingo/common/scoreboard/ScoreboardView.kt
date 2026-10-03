package me.jfenn.bingo.common.scoreboard

import me.jfenn.bingo.platform.scoreboard.ScoreChange
import me.jfenn.bingo.platform.text.IText
import java.util.*

class ScoreboardView(
    val objectiveName: String,
) {

    var players = setOf<UUID>()

    private var contents: Map<String, Int> = emptyMap()

    class ScoreboardItem(
        val string: String,
        val text: IText,
        val score: IText?,
    )

    fun createChanges(list: List<ScoreboardItem>): List<ScoreChange>
        = list.mapIndexed { i, item -> ScoreChange.Create(item.string, item.text, item.score, list.size - i) }

    fun updateContents(newList: List<ScoreboardItem>): List<ScoreChange> {
        val changes = mutableListOf<ScoreChange>()

        for ((i, newText) in newList.withIndex()) {
            val newIndex = newList.size - i
            val prevIndex = contents[newText.string]
            if (prevIndex == newIndex) continue

            if (prevIndex != null) {
                changes.add(ScoreChange.Update(newText.string, newText.text, newText.score, newIndex))
            } else {
                changes.add(ScoreChange.Create(newText.string, newText.text, newText.score, newIndex))
            }
        }

        for ((oldString, i) in contents) {
            if (newList.none { it.string == oldString }) {
                changes.add(ScoreChange.Remove(oldString, null, null, i))
            }
        }

        contents = newList.withIndex()
            .associate { (i, text) ->
                text.string to (newList.size - i)
            }

        return changes
    }

}