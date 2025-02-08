package me.jfenn.bingo.client.platform.event.model

import me.jfenn.bingo.client.platform.screen.IScreenHelper

class ScreenEvent(
    val type: ScreenType,
    val screen: IScreenHelper,
)

enum class ScreenType {
    TitleScreen,
    Other,
}
