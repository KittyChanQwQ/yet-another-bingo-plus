package me.jfenn.bingo.common.scoreboard

import java.util.*

class ScoreboardView {

    var players = setOf<UUID>()

    private var contents: Map<String, Int> = emptyMap()

    fun createChanges(list: List<String>): List<ScoreChange>
        = list.mapIndexed { i, text -> ScoreChange.Create(text, list.size - i) }

    fun updateContents(newList: List<String>): List<ScoreChange> {
        val changes = mutableListOf<ScoreChange>()

        for ((i, newText) in newList.withIndex()) {
            val newIndex = newList.size - i
            val prevIndex = contents[newText]
            if (prevIndex == newIndex) continue

            if (prevIndex != null) {
                changes.add(ScoreChange.Update(newText, newIndex))
            } else {
                changes.add(ScoreChange.Create(newText, newIndex))
            }
        }

        for ((oldText, i) in contents) {
            if (!newList.contains(oldText)) {
                changes.add(ScoreChange.Remove(oldText, i))
            }
        }

        contents = newList.withIndex()
            .associate { (i, text) ->
                text to (newList.size - i)
            }

        return changes
    }

}