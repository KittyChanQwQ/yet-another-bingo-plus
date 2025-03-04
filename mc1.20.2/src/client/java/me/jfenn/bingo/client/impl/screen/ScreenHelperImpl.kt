package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.impl.screen.button.ButtonImpl
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IDrawable
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder

class ScreenHelperImpl(
    override val screen: Screen,
) : IScreenHelper {
    override val width get() = screen.width
    override val height get() = screen.height

    override fun addButton(button: IButton) {
        require(button is ButtonImpl)
        Screens.getButtons(screen).add(button.button)
    }

    override fun addDrawable(drawable: IDrawable) {
        screen.addDrawableChild(object : Drawable, Element, Selectable {
            override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
                drawable.render(DrawService(context))
            }

            override fun setFocused(focused: Boolean) {}
            override fun isFocused(): Boolean = false
            override fun appendNarrations(builder: NarrationMessageBuilder?) {}
            override fun getType(): Selectable.SelectionType = Selectable.SelectionType.NONE
        })
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
