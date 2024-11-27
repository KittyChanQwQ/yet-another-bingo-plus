package me.jfenn.bingo.common.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text

@Serializable
sealed class EndWhen {
    @Serializable
    @SerialName("never")
    data object Never : EndWhen() {
        override fun string(text: TextProvider): Text = text.string(StringKey.OptionsEndWhenNever)
    }

    @Serializable
    @SerialName("first_win")
    data object FirstWin : EndWhen() {
        override fun string(text: TextProvider): Text = text.string(StringKey.OptionsEndWhenFirstWin)
    }

    @Serializable
    @SerialName("teams_win")
    data class TeamsWin(val teams: Int) : EndWhen() {
        override fun string(text: TextProvider): Text = text.string(StringKey.OptionsEndWhenTeamsWin, teams)
    }

    @Serializable
    @SerialName("all_win")
    data object AllWin : EndWhen() {
        override fun string(text: TextProvider): Text = text.string(StringKey.OptionsEndWhenAllWin)
    }

    abstract fun string(text: TextProvider): Text
}