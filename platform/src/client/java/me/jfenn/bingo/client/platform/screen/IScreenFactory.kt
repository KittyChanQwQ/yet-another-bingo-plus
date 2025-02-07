package me.jfenn.bingo.client.platform.screen

import me.jfenn.bingo.client.platform.text.IText
import net.minecraft.client.gui.screen.Screen

interface IScreenFactory {
    fun build(title: IText, factory: (IScreenHelper) -> IScreen): Screen
}