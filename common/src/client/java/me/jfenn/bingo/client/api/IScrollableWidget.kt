package me.jfenn.bingo.client.api

import net.minecraft.client.gui.widget.ScrollableWidget

abstract class IScrollableWidget {
    var x: Int = 0
    var y: Int = 0
    var width: Int = 0
    var height: Int = 0
    var padding: Int = 4
    var widget: ScrollableWidget? = null

    val contentWidth get() = width - padding*2

    abstract fun measureContentHeight(): Int
    abstract fun renderContents(drawService: IDrawService, mouseX: Int, mouseY: Int)
}

interface IScrollableWidgetFactory {
    fun create(widget: IScrollableWidget): IScrollableWidget
}
