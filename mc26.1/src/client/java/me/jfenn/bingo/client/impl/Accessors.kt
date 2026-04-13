package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.mixin.CreateWorldScreenAccessor
import me.jfenn.bingo.client.mixin.DrawContextAccessor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen

val CreateWorldScreen.accessor get() = this as CreateWorldScreenAccessor
val GuiGraphicsExtractor.accessor get() = this as DrawContextAccessor
