package me.jfenn.bingo.common.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.text.IText

@Serializable
sealed interface GameEndReason {
    val isForfeit: Boolean get() = false
    fun format(text: TextProvider): IText = text.string(StringKey.GameEndGameOver)

    @Serializable
    @SerialName("bingo")
    data object Bingo : GameEndReason {
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonBingo)
        }
    }

    @Serializable
    @SerialName("lockout")
    data object Lockout : GameEndReason {
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonLockout)
        }
    }

    @Serializable
    @SerialName("stalemate")
    data object Stalemate : GameEndReason {
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonStalemate)
        }
    }

    @Serializable
    @SerialName("impossible_goal")
    data object ImpossibleGoal : GameEndReason {
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonImpossibleGoal)
        }
    }

    @Serializable
    @SerialName("timer")
    data object Timer : GameEndReason {
        override val isForfeit = true
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonTimer)
        }
    }

    @Serializable
    @SerialName("command")
    class Command(val playerName: String) : GameEndReason {
        override val isForfeit = true
        override fun format(text: TextProvider): IText {
            return text.string(StringKey.GameEndReasonCommand, playerName)
        }
    }

    @Serializable
    @SerialName("error")
    data object Error : GameEndReason {
        override val isForfeit = true
    }
}