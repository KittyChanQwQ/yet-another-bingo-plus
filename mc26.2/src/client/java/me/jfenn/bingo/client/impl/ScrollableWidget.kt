package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.impl.draw.DrawService
import me.jfenn.bingo.client.platform.IScrollableWidget
import me.jfenn.bingo.client.platform.IScrollableWidgetFactory
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractTextAreaWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

class ScrollableWidget(
    private val content: IScrollableWidget,
) : AbstractTextAreaWidget(0, 0, 0, 0, Component.empty(), defaultSettings(9)) {
    // override the hardcoded padding values
    private var contentPadding = 4

    override fun totalInnerPadding(): Int {
        return contentPadding
    }

    override fun getInnerHeight(): Int {
        return content.measureContentHeight()
    }

    // don't draw the box background
    override fun extractBackground(graphics: GuiGraphicsExtractor) {}

    override fun updateWidgetNarration(output: NarrationElementOutput) {}

    override fun scrollRate(): Double {
        return 9.0
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        this.x = content.x
        this.y = content.y
        this.width = content.width
        this.height = content.height
        this.contentPadding = content.padding

        super.extractWidgetRenderState(graphics, mouseX, mouseY, a)
    }

    override fun extractContents(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val drawService = DrawService(graphics)
        drawService.delta = delta

        if (content.background != 0) {
            graphics.fill(x, y, x + width, y + innerHeight + contentPadding*2, content.background)
        }

        graphics.pose().pushMatrix()
        graphics.pose().translate((x + contentPadding).toFloat(), (y + contentPadding).toFloat())
        content.renderContents(drawService, mouseX - (x + contentPadding), mouseY - (y + contentPadding))
        graphics.pose().popMatrix()
    }
}

object ScrollableWidgetFactory : IScrollableWidgetFactory {
    override fun create(widget: IScrollableWidget): IScrollableWidget {
        widget.widget = ScrollableWidget(widget)
        return widget
    }
}
