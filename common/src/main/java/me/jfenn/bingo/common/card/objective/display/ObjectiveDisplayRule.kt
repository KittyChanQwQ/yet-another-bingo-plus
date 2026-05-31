package me.jfenn.bingo.common.card.objective.display

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.common.card.objective.BingoObjective

@Serializable
internal data class ObjectiveDisplayRule(
    val predicate: Predicate,
    val display: ObjectiveDisplay.Data,
) {
    @Serializable
    sealed interface Predicate {
        fun matches(obj: BingoObjective): Boolean
    }

    @Serializable
    @SerialName("match_identifier")
    data class MatchIdentifier(val identifier: String) : Predicate {
        @Transient
        val regex = identifier.split('*')
            .joinToString(".*") { Regex.escape(it) }
            .let { "^$it$".toRegex() }

        override fun matches(obj: BingoObjective): Boolean = regex.matches(obj.id)
    }
}
