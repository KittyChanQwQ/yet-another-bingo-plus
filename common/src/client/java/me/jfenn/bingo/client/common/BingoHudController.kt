package me.jfenn.bingo.client.common

import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.common.PACKET_CARD_DISPLAY
import me.jfenn.bingo.common.PACKET_CARD_UPDATE
import me.jfenn.bingo.common.card.BingoTeamKey
import me.jfenn.bingo.common.map.CardTeams
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.scope.BingoComponent
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class BingoHudController(
    private val drawServiceFactory: IDrawServiceFactory,
) : BingoComponent() {

    private var display: CardTeams? = null
    private val views = mutableMapOf<BingoTeamKey?, CardView>()

    companion object {
        const val CARD_WIDTH = 118
        const val CARD_HEIGHT = 128
    }

    private fun drawCard(context: DrawContext, service: IDrawService, view: CardView, x: Int, y: Int) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        service.drawTexture(
            texture = Identifier("minecraft", "bingo/hud_card"),
            x = x,
            y = y,
            u = 0f,
            v = 0f,
            width = CARD_WIDTH,
            height = CARD_HEIGHT,
            textureWidth = CARD_WIDTH,
            textureHeight = CARD_HEIGHT,
        )

        view.teamColor?.let { color ->
            service.drawTexture(
                texture = Identifier("minecraft", "bingo/hud_frame_${color.name.lowercase()}"),
                x = x, y = y,
                u = 0f, v = 0f,
                width = CARD_WIDTH,
                height = CARD_HEIGHT,
                textureWidth = CARD_WIDTH,
                textureHeight = CARD_HEIGHT,
            )
        }

        val teamNameColor = when (view.teamColor?.isDark) {
            true -> 0xa8ffffff.toInt()
            false -> 0xa8000000.toInt()
            else -> 0xff505050.toInt()
        }
        context.drawText(textRenderer, "Bingo Card", x + 10, y + 8, teamNameColor, false)

        for (tileX in 0..4) for (tileY in 0..4) {
            val tile = view.tile(tileX, tileY) ?: continue
            val itemX = 10 + x + tileX*20
            val itemY = 20 + y + tileY*20

            when {
                tile.isFlashing -> context.fill(itemX, itemY, itemX + 18, itemY + 18, 0xffb24cd8.toInt())
                tile.isAchieved -> context.fill(itemX, itemY, itemX + 18, itemY + 18, 0xff55FF55.toInt())
            }

            if (tile.isAdvancement) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_advancement"),
                    x = itemX - 1, y = itemY - 1,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isHidden) {
                val texture = tile.itemTier
                    ?.let { Identifier("minecraft", "bingo/image_hidden_${it.name.lowercase()}") }
                    ?: Identifier("minecraft", "bingo/image_hidden")

                service.drawTexture(
                    texture = texture,
                    x = itemX + 1, y = itemY + 1,
                    u = 0f, v = 0f,
                    width = 16, height = 16,
                    textureWidth = 16, textureHeight = 16,
                )
            }

            if (tile.item != null) {
                context.drawItem(tile.item, itemX + 1, itemY + 1)
                context.drawItemInSlot(textRenderer, tile.item, itemX, itemY)
            }

            if (tile.isAchieved && !tile.isFlashing)
                context.fill(RenderLayer.getGuiOverlay(), itemX, itemY, itemX + 18, itemY + 18, 0x4055FF55)

            if (tile.isLocked) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_locked"),
                    x = itemX - 1, y = itemY - 1,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            for ((i, teamColor) in tile.teamColors.withIndex()) {
                val colorValue = teamColor.colorValue?.or(0xff000000.toInt())
                    ?: continue

                val start = i * 34f / tile.teamColors.size
                val end = (i+1) * 34f / tile.teamColors.size

                if (start < 17f) {
                    context.drawHorizontalLine(itemX + start.toInt(), itemX + 17, itemY, colorValue)
                    context.drawVerticalLine(itemX, itemY + start.toInt(), itemY + 17, colorValue)
                }

                if (end > 17f) {
                    context.drawHorizontalLine(itemX, itemX + end.toInt() - 17, itemY + 17, colorValue)
                    context.drawVerticalLine(itemX + 17, itemY, itemY + end.toInt() - 17, colorValue)
                }
            }
        }
    }

    private fun drawHud(context: DrawContext) {
        val drawService = drawServiceFactory.create(context)
        val teamKeys = display?.display ?: return

        for ((i, teamKey) in teamKeys.withIndex()) {
            val view = views[teamKey] ?: continue
            drawCard(context, drawService, view, i*CARD_WIDTH, 0)
        }
    }

    init {
        ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_DISPLAY) { client, handler, buf, response ->
            val display = CardTeams.fromPacketBuf(buf)
            client.execute { this.display = display }
        }

        ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_UPDATE) { client, handler, buf, response ->
            val view = CardView.fromPacketBuf(buf)
            client.execute { views[view.teamKey] = view }
        }

        HudRenderCallback.EVENT.register { context, tickDelta ->
            drawHud(context)
        }

        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            client.execute {
                display = null
                views.clear()
            }
        }
    }
}