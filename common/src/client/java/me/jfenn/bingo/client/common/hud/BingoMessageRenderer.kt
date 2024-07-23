package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BingoMessageRenderer(
    private val state: BingoHudState,
    private val config: BingoConfig,
) {

    companion object {
        const val ITEM_HEIGHT = 18

        val ICON_HIDDEN_ID = Identifier.of("minecraft", "bingo/message_item_hidden")!!
        val ICON_CHECK_ID = Identifier.of("minecraft", "bingo/message_check")!!
        val ICON_LEADING_ID = Identifier.of("minecraft", "bingo/message_leading")!!
    }

    private val fadeInDuration = 500.milliseconds

    fun drawMessages(
        drawService: IDrawService,
        x: Int,
        y: Int,
        z: Int,
        messageScale: Float,
        isAboveCard: Boolean,
    ) {
        val context = drawService.context
        val direction = if (isAboveCard) -1 else 1
        val now = Clock.System.now()
        val duration = config.client.messageDurationSeconds.seconds

        context.matrices.push()
        context.matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
        context.matrices.scale(messageScale, messageScale, 1f)

        var itemY = if (isAboveCard) -ITEM_HEIGHT else 0
        for (message in state.messages.reversed().take(5)) {
            val timeSinceCreated = now - message.createdAt
            val fadeIn = (timeSinceCreated / fadeInDuration).toFloat().coerceIn(0f, 1f)
            val fadeOut = (1f - ((timeSinceCreated - duration) / fadeInDuration).toFloat()).coerceIn(0f, 1f)
            val visibility = fadeIn * fadeOut

            if (timeSinceCreated > (duration + fadeInDuration)) {
                // if the item is no longer visible and its duration is over, remove it from the list
                state.messages.remove(message)
                continue
            }

            context.matrices.push()
            context.matrices.translate(0f, itemY.toFloat() + (1f - fadeOut).pow(2) * ITEM_HEIGHT * direction, 0f)

            drawMessage(drawService, message, visibility)

            context.matrices.pop()

            itemY += (ITEM_HEIGHT * direction * fadeIn.pow(2)).roundToInt()
        }

        context.matrices.pop()
    }

    private fun drawMessage(
        drawService: IDrawService,
        message: BingoHudState.ScoreMessage,
        visibility: Float,
    ) {
        val context = drawService.context
        val textRenderer = MinecraftClient.getInstance().textRenderer

        context.setShaderColor(1f, 1f, 1f, visibility.pow(2))

        val itemStack = message.packet.item?.stack
        if (itemStack != null) {
            context.drawItem(itemStack, 8, ITEM_HEIGHT/2 - 8)
        } else {
            val iconTexture = when (message.packet.messageType) {
                ScoreMessagePacket.MessageType.LEADING_TEAM -> ICON_LEADING_ID
                ScoreMessagePacket.MessageType.CARD_COMPLETED,
                ScoreMessagePacket.MessageType.LINE_SCORED -> ICON_CHECK_ID
                ScoreMessagePacket.MessageType.ITEM_SCORED -> ICON_HIDDEN_ID
                else -> null
            }

            if (iconTexture != null) {
                drawService.drawTexture(
                    texture = iconTexture,
                    x = 8, y = ITEM_HEIGHT/2 - 8,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }
        }

        context.drawText(
            textRenderer,
            message.packet.message,
            28, ITEM_HEIGHT/2 - textRenderer.fontHeight/2,
            0xFF_FFFFFF.toInt(),
            true,
        )

        context.setShaderColor(1f, 1f, 1f, 1f)
    }

}