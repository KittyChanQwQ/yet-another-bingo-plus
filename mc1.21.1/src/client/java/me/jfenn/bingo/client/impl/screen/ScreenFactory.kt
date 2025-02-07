package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import me.jfenn.bingo.client.platform.text.IText
import net.minecraft.client.gui.screen.Screen

class ScreenFactory : IScreenFactory {
    override fun build(title: IText, factory: (IScreenHelper) -> IScreen): Screen {
        val screen = ScreenImpl(title.value)
        screen.impl = factory(screen.helper)
        return screen
    }
}