package me.jfenn.bingo.common.menu

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import me.jfenn.bingo.platform.text.IText

@Serializable
class MenuState(
    var page: MenuPage = MenuPage.ROOT,
) {
    @Transient
    var warnings = listOf<IText>()
}

@Serializable
enum class MenuPage {
    ROOT,
    FEATURES,
    GOAL,
    ITEMS,
    WARNINGS,
}
