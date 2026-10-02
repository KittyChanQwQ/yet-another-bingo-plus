package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.platform.screen.IScreen
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.absoluteValue

class ScreenImpl(
    title: Component,
) : Screen(title) {
    var impl: IScreen? = null
    var parent: Screen? = null
    val helper = MutableScreenHelperImpl(this)

    override fun init() {
        super.init()
        impl?.init()
    }

    fun bingoAddDrawableChild(drawable: Renderable) {
        require(drawable is GuiEventListener) { "${drawable::class.simpleName} is not Element!" }
        require(drawable is NarratableEntry) { "${drawable::class.simpleName} is not Element!" }
        addRenderableWidget(drawable)
    }

    fun bingoClearChildren() {
        clearWidgets()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        impl?.resize(width, height)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val service = DrawService(graphics)
        impl?.beforeRender(service)
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        impl?.render(service, mouseX, mouseY, delta)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        return impl?.mouseDragged(click.x, click.y) == true || super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        return impl?.mouseReleased(click.x, click.y) == true || super.mouseReleased(click)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        return impl?.mouseClicked(click.x, click.y, click.button()) == true || super.mouseClicked(click, doubled)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        return impl?.keyPressed(KeyInputImpl(input)) == true || super.keyPressed(input)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val amount = arrayOf(horizontalAmount, verticalAmount).maxBy { it.absoluteValue }
        return impl?.mouseScrolled(mouseX, mouseY, amount) == true || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun isPauseScreen(): Boolean {
        return impl?.shouldPause() ?: false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return impl?.shouldCloseOnEsc() ?: true
    }

    override fun onClose() {
        impl?.onClose()
        minecraft.setScreen(parent)
    }

    override fun added() {
        super.added()
        impl?.added()
    }

    override fun removed() {
        super.removed()
        impl?.removed()
    }
}