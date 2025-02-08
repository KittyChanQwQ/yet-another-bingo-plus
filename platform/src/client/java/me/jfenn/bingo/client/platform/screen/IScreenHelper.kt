package me.jfenn.bingo.client.platform.screen

import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.screen.Screen

interface IScreenHelper {
    val screen: Screen
    val width: Int
    val height: Int
    fun addDrawableChild(drawable: Drawable)
    fun addButton(button: IButton)
    fun clearChildren()
    fun close()
}