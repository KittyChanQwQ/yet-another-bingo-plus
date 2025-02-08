package me.jfenn.bingo.client.platform.screen

import me.jfenn.bingo.client.platform.renderer.IDrawService

interface IScreen {
    fun init() = Unit
    fun resize(width: Int, height: Int) = Unit
    fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float)
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = false
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean = false
    fun shouldPause(): Boolean = false
    fun shouldCloseOnEsc(): Boolean = true
    fun onClose(): Unit = Unit
}