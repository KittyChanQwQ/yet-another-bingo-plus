package me.jfenn.bingo.client.impl.screen

import me.jfenn.bingo.client.impl.DrawService
import me.jfenn.bingo.client.platform.screen.IScreen
import me.jfenn.bingo.client.platform.screen.IScreenHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class ScreenImpl(title: Text) : Screen(title) {
    var impl: IScreen? = null
    val helper = object : IScreenHelper {
        override val screen: Screen
            get() = this@ScreenImpl
        override val width by this@ScreenImpl::width
        override val height by this@ScreenImpl::height

        override fun addDrawableChild(drawable: Drawable) {
            require(drawable is Element) { "${drawable::class.simpleName} is not Element!" }
            require(drawable is Selectable) { "${drawable::class.simpleName} is not Element!" }
            this@ScreenImpl.addDrawableChild(drawable)
        }

        override fun clearChildren() {
            this@ScreenImpl.clearChildren()
        }

        override fun close() {
            this@ScreenImpl.close()
        }
    }

    override fun init() {
        super.init()
        impl?.init()
    }

    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        super.resize(client, width, height)
        impl?.resize(width, height)
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        if (context == null) return
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
        impl?.render(DrawService(context), mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return impl?.mouseClicked(mouseX, mouseY, button) == true || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return impl?.mouseScrolled(mouseX, mouseY, amount) == true || super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun shouldPause(): Boolean {
        return impl?.shouldPause() ?: false
    }

    override fun shouldCloseOnEsc(): Boolean {
        return impl?.shouldCloseOnEsc() ?: true
    }

    override fun close() {
        impl?.onClose()
        super.close()
    }
}