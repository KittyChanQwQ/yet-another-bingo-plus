package me.jfenn.bingo.common.options

import me.jfenn.bingo.api.options.BingoGoal
import me.jfenn.bingo.api.options.EndWhen
import me.jfenn.bingo.api.options.StalemateBehavior
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.IText

fun BingoGoal.string(): StringKey {
    return when (this) {
        is BingoGoal.Items -> StringKey.GoalItems
        is BingoGoal.Lines -> StringKey.GoalLines
    }
}


fun BingoGoal.format(textProvider: TextProvider, supportsFullCard: Boolean = true, descriptive: Boolean = false): IText {
    return when {
        supportsFullCard && this.isFullCard() -> textProvider.string(StringKey.GoalFullCard)
            .also {
                if (descriptive) {
                    it.append(" ")
                    it.append(textProvider.string(string()).bracketed())
                }
            }
        else -> when (this) {
            is BingoGoal.Items -> textProvider.itemCount(this.items)
            is BingoGoal.Lines -> textProvider.lineCount(this.lines)
        }
    }
}

fun EndWhen.string(text: TextProvider) = when (this) {
    is EndWhen.Never -> text.string(StringKey.OptionsEndWhenNever)
    is EndWhen.FirstWin -> text.string(StringKey.OptionsEndWhenFirstWin)
    is EndWhen.TeamsWin -> text.string(StringKey.OptionsEndWhenTeamsWin, teams)
    is EndWhen.AllWin -> text.string(StringKey.OptionsEndWhenAllWin)
}

val StalemateBehavior.string get() = when (this) {
    StalemateBehavior.END_GAME -> StringKey.OptionsWinBehaviorStalemateEndGame
    StalemateBehavior.REROLL_CARD -> StringKey.OptionsWinBehaviorStalemateRerollCard
    StalemateBehavior.NOTHING -> StringKey.OptionsWinBehaviorStalemateDoNothing
}

fun StalemateBehavior.string(text: TextProvider) = text.string(StringKey.OptionsWinBehaviorStalemate, string)

val LocatorBarMode.string get() = when (this) {
    LocatorBarMode.ALL -> StringKey.OptionsLocatorBarAll
    LocatorBarMode.TEAM_AND_WINNER -> StringKey.OptionsLocatorBarTeamAndWinner
    LocatorBarMode.TEAM_ONLY -> StringKey.OptionsLocatorBarTeamOnly
}

fun LocatorBarMode.string(text: TextProvider) = text.string(StringKey.OptionsLocatorBarMode, string)
