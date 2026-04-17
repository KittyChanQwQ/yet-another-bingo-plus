package me.jfenn.bingo.common.options

import kotlinx.serialization.Serializable
import me.jfenn.bingo.api.options.BingoGoal
import me.jfenn.bingo.api.options.BingoWinCondition
import me.jfenn.bingo.api.options.EndWhen
import me.jfenn.bingo.api.options.StalemateBehavior
import me.jfenn.bingo.common.utils.DurationType

@Serializable
internal sealed interface RestoreOption {
    fun apply(options: BingoOptions)
}

@Serializable
internal data class RestoreGoal(val goal: BingoGoal) : RestoreOption {
    override fun apply(options: BingoOptions) {
        options.cards.forEach { it.goal = goal }
    }
}

@Serializable
internal data class RestoreStalemateBehavior(val stalemateBehavior: StalemateBehavior) : RestoreOption {
    override fun apply(options: BingoOptions) {
        options.stalemateBehavior = stalemateBehavior
    }
}

@Serializable
internal data class RestoreWinCondition(val winCondition: BingoWinCondition) : RestoreOption {
    override fun apply(options: BingoOptions) {
        options.winCondition = winCondition
    }
}

@Serializable
internal data class RestoreEndWhen(val endGameWhen: EndWhen) : RestoreOption {
    override fun apply(options: BingoOptions) {
        options.endGameWhen = endGameWhen
    }
}

@Serializable
internal data class RestoreTimeLimit(val timeLimit: DurationType?) : RestoreOption {
    override fun apply(options: BingoOptions) {
        options.timeLimit = timeLimit
    }
}
