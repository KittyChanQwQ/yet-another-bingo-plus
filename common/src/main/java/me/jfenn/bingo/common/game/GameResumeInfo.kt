package me.jfenn.bingo.common.game

import me.jfenn.bingo.api.options.BingoGoal
import me.jfenn.bingo.common.utils.DurationType
import java.util.*

sealed interface GameResumeInfo {
    data object KeepPlaying : GameResumeInfo
    data class IncreaseGoal(val cardId: UUID, val goal: BingoGoal) : GameResumeInfo
    data class IncreaseTimeLimit(val timeLimit: DurationType) : GameResumeInfo
    data object WaitUntilAllWin : GameResumeInfo
    data object InfiniteMode : GameResumeInfo
}