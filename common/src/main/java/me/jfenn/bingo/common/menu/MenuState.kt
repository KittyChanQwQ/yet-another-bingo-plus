package me.jfenn.bingo.common.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.jfenn.bingo.platform.text.IText

@Serializable
sealed interface MenuState {
    val page: MenuPage

    @Serializable
    @SerialName("root")
    data object Root : MenuState {
        override val page: MenuPage = MenuPage.ROOT
    }

    @Serializable
    @SerialName("features")
    data object Features : MenuState {
        override val page: MenuPage = MenuPage.FEATURES
    }

    @Serializable
    @SerialName("goal")
    data object Goal : MenuState {
        override val page: MenuPage = MenuPage.GOAL
    }

    @Serializable
    @SerialName("items")
    data object Items : MenuState {
        override val page: MenuPage = MenuPage.ITEMS
    }

    @Serializable
    @SerialName("warnings")
    data class Warnings(
        val warnings: List<IText>
    ) : MenuState {
        override val page: MenuPage = MenuPage.PROMPT
    }

    @Serializable
    @SerialName("reset_prompt")
    data object ResetPrompt : MenuState {
        override val page: MenuPage = MenuPage.PROMPT
    }
}

enum class MenuPage {
    ROOT,
    FEATURES,
    GOAL,
    ITEMS,
    PROMPT,
}
