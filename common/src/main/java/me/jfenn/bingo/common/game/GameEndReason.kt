package me.jfenn.bingo.common.game

import me.jfenn.bingo.generated.StringKey

enum class GameEndReason(
    val stringKey: StringKey,
) {
    BINGO(StringKey.GameEndReasonBingo),
    LOCKOUT(StringKey.GameEndReasonLockout),
    TIMER(StringKey.GameEndReasonTimer),
}