package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenFactory
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ScreenFactory : IScreenFactory {
    override fun build(title: Text, factory: (IScreenHelper) -> IScreen): Screen {
        val screen = ScreenImpl(title)
        screen.impl = factory(screen.helper)
        return screen
    }
}