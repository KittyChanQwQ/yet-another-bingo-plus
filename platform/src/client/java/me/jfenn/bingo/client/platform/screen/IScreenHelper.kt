package me.jfenn.bingo.client.platform.screen

import me.jfenn.bingo.client.platform.renderer.IDrawService
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.screen.Screen

interface IScreenHelper {
    val screen: Screen
    val width: Int
    val height: Int
    fun addDrawableChild(drawable: Drawable)
    fun addDrawable(drawable: IDrawable)
    fun addButton(button: IButton)
    fun clearChildren()
    fun close()
}

interface IDrawable {
    fun render(drawService: IDrawService)
}
