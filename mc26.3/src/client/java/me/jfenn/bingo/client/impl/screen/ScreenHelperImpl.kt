package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.impl.screen.button.ButtonImpl
import me.jfenn.bingo.client.platform.screen.IButton
import me.jfenn.bingo.client.platform.screen.IDrawable
import me.jfenn.bingo.client.platform.screen.IMutableScreenHelper
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import org.joml.Vector2i

open class ScreenHelperImpl(
    override val screen: Screen,
) : IScreenHelper {
    override val width get() = screen.width
    override val height get() = screen.height

    companion object {
        private const val MOUSE_BUTTON_LEFT = 1
    }

    override fun onAfterLeftClick(callback: (Vector2i) -> Unit) {
        ScreenMouseEvents.afterMouseClick(screen).register { _, click, _ ->
            if (click.button() == MOUSE_BUTTON_LEFT) {
                callback(Vector2i(click.x.toInt(), click.y.toInt()))
                true
            } else false
        }
    }

    override fun addButton(button: IButton) {
        require(button is ButtonImpl)
        Screens.getWidgets(screen).add(button.button)
    }

    override fun close() {
        screen.onClose()
    }
}

class MutableScreenHelperImpl(
    private val screenImpl: ScreenImpl,
) : ScreenHelperImpl(screenImpl), IMutableScreenHelper {
    override fun addDrawable(drawable: IDrawable) {
        screenImpl.bingoAddDrawableChild(object : Renderable, GuiEventListener, NarratableEntry {
            override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
                drawable.render(DrawService(graphics))
            }

            override fun setFocused(focused: Boolean) {}
            override fun isFocused(): Boolean = false
            override fun updateNarration(output: NarrationElementOutput) {}
            override fun narrationPriority(): NarratableEntry.NarrationPriority = NarratableEntry.NarrationPriority.NONE
        })
    }

    override fun addDrawableChild(drawable: Renderable) {
        screenImpl.bingoAddDrawableChild(drawable)
    }

    override fun clearChildren() {
        screenImpl.bingoClearChildren()
    }
}
