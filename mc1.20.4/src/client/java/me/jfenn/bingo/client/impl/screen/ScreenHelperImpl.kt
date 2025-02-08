package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.impl.ButtonImpl
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen

class ScreenHelperImpl(
    override val screen: Screen,
) : IScreenHelper {
    override val width get() = screen.width
    override val height get() = screen.height

    override fun addButton(button: IButton) {
        require(button is ButtonImpl)
        Screens.getButtons(screen).add(button.button)
    }

    override fun addDrawableChild(drawable: Drawable) {
        require(drawable is Element) { "${drawable::class.simpleName} is not Element!" }
        require(drawable is Selectable) { "${drawable::class.simpleName} is not Element!" }
        screen.addDrawableChild(drawable)
    }

    override fun clearChildren() {
        screen.clearChildren()
    }

    override fun close() {
        screen.close()
    }
}
