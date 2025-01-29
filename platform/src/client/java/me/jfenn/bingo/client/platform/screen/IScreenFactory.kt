package me.jfenn.bingo.client.platform.screen

import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

interface IScreenFactory {
    fun build(title: Text, factory: (IScreenHelper) -> IScreen): Screen
}