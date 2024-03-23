package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.utils.json
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.resource.ResourceType
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.profiler.Profiler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.jvm.optionals.getOrNull

class BingoHudRenderer {

    companion object {
        const val CARD_WIDTH = 118
        const val CARD_HEIGHT = 128
    }

    private var colorMap = emptyMap<String, String>()

    private fun textColor(color: Formatting?): Int {
        (colorMap[color?.name ?: "null"] ?: colorMap["null"])
            ?.substring(1)
            ?.toUIntOrNull(16)
            ?.let { return it.toInt() }

        return when (color) {
            Formatting.GOLD -> false
            Formatting.DARK_GRAY -> true
            Formatting.BLUE -> true
            Formatting.GREEN -> false
            Formatting.AQUA -> false
            Formatting.RED -> true
            Formatting.LIGHT_PURPLE -> false
            Formatting.YELLOW -> false
            else -> false
        }.let {
            when (it) {
                true -> 0xa8ffffff.toInt()
                false -> 0xa8000000.toInt()
            }
        }
    }

    fun getItem(
        x: Int,
        y: Int,
        cardScale: Float,
        mouseX: Int,
        mouseY: Int,
    ): Pair<Int, Int>? {
        val startX = x + (10 * cardScale)
        val startY = y + (20 * cardScale)

        val tileX = (mouseX - startX).div(20 * cardScale).toInt()
        val tileY = (mouseY - startY).div(20 * cardScale).toInt()

        if (tileX !in 0..4 || tileY !in 0..4)
            return null

        return Pair(tileX, tileY)
    }

    fun drawCard(
        context: DrawContext,
        service: IDrawService,
        view: CardView,
        x: Int,
        y: Int,
        z: Int = 0,
        cardScale: Float,
        mouseX: Int = -1,
        mouseY: Int = -1,
        showTeamOutlines: Boolean = true,
    ) {
        val textRenderer = MinecraftClient.getInstance().textRenderer

        context.matrices.push()
        context.matrices.translate(x.toFloat(), y.toFloat(), z.toFloat())
        context.matrices.scale(cardScale, cardScale, cardScale)

        // Draw a translucent background behind all card items
        context.fill(10, 20, 10 + 5*20, 20 + 5*20, 0x80505050.toInt())

        service.drawTexture(
            texture = Identifier("minecraft", "bingo/hud_card"),
            x = 0,
            y = 0,
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
                x = 0, y = 0,
                u = 0f, v = 0f,
                width = CARD_WIDTH,
                height = CARD_HEIGHT,
                textureWidth = CARD_WIDTH,
                textureHeight = CARD_HEIGHT,
            )
        }

        val teamNameColor = textColor(view.teamColor)
        context.drawText(textRenderer, view.teamName, 10, 8, teamNameColor, false)

        for (tileX in 0..4) for (tileY in 0..4) {
            val tile = view.tile(tileX, tileY) ?: continue
            val itemX = 10 + tileX*20
            val itemY = 20 + tileY*20

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
                // always draws centered at z=150... but 3d items can extend past this
                // so any overlays should start at z=300 to be safe
                context.drawItem(tile.item, itemX + 1, itemY + 1, itemX + itemY * CARD_WIDTH)
                // draws the item count + durability bar
                context.drawItemInSlot(textRenderer, tile.item, itemX, itemY)
            }

            if (tile.isAchieved && !tile.isFlashing && mouseX == -1 && mouseY == -1)
                context.fill(RenderLayer.getGuiOverlay(), itemX, itemY, itemX + 18, itemY + 18, 0x4055FF55)

            if (tile.isMultiItem) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_multi"),
                    x = itemX - 1, y = itemY - 1, z = 300,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (tile.isLocked) {
                service.drawTexture(
                    texture = Identifier("minecraft", "bingo/image_frame_locked"),
                    x = itemX - 1, y = itemY - 1, z = 301,
                    u = 0f, v = 0f,
                    width = 20, height = 20,
                    textureWidth = 20, textureHeight = 20,
                )
            }

            if (showTeamOutlines) for ((i, teamColor) in tile.teamColors.withIndex()) {
                val color = Color.fromInt(teamColor.colorValue ?: continue)
                    .copy(a = 255)
                val colorValue = color.toInt()
                val colorValueDark = color.mix(Color.fromInt(0x40_000000)).toInt()

                val start = i * 38f / tile.teamColors.size
                val end = (i+1) * 38f / tile.teamColors.size

                if (start < 19f) {
                    val lineStart = start.toInt()
                    val lineEnd = end.toInt().coerceAtMost(19)
                    context.drawHorizontalLine((itemX-1) + lineStart, (itemX-1) + lineEnd, (itemY-1), colorValue)
                    context.drawVerticalLine((itemX-1), (itemY-1) + lineStart, (itemY-1) + lineEnd, colorValue)
                }

                if (end > 19f) {
                    val lineStart = (start.toInt() - 19).coerceAtLeast(0)
                    val lineEnd = end.toInt() - 19
                    context.drawHorizontalLine((itemX-1) + lineStart, (itemX-1) + lineEnd, (itemY-1) + 19, colorValueDark)
                    context.drawVerticalLine((itemX-1) + 19, (itemY-1) + lineStart, (itemY-1) + lineEnd, colorValueDark)
                }

                val start2 = i * 34f / tile.teamColors.size
                val end2 = (i+1) * 34f / tile.teamColors.size

                if (start2 < 17f) {
                    val lineStart = start2.toInt()
                    val lineEnd = end2.toInt().coerceAtMost(17)
                    context.drawHorizontalLine((itemX) + lineStart, (itemX) + lineEnd, itemY, colorValueDark)
                    context.drawVerticalLine(itemX, (itemY) + lineStart, (itemY) + lineEnd, colorValueDark)
                }

                if (end2 > 17f) {
                    val lineStart = (start2.toInt() - 17).coerceAtLeast(0)
                    val lineEnd = end2.toInt() - 17
                    context.drawHorizontalLine((itemX) + lineStart, (itemX) + lineEnd, (itemY) + 17, colorValue)
                    context.drawVerticalLine((itemX) + 17, (itemY) + lineStart, (itemY) + lineEnd, colorValue)
                }
            }
        }

        context.matrices.pop()

        // Render the hover tooltips last, without the matrix translate/transform
        val (tileX, tileY) = getItem(x, y, cardScale, mouseX, mouseY) ?: return
        val tile = view.tile(tileX, tileY) ?: return
        val itemX = 10 + tileX * 20
        val itemY = 20 + tileY * 20

        val mouseItemX = x + itemX * cardScale
        val mouseItemRangeX = mouseItemX.toInt() until (mouseItemX + 20*cardScale).toInt()
        val mouseItemY = y + itemY * cardScale
        val mouseItemRangeY = mouseItemY.toInt() until (mouseItemY + 20*cardScale).toInt()

        if (tile.item != null && mouseX in mouseItemRangeX && mouseY in mouseItemRangeY) {
            context.drawItemTooltip(textRenderer, tile.item, mouseX, mouseY)
        }
    }

    init {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun reload(manager: ResourceManager) {
                val colorMap = manager.getResource(Identifier(MOD_ID_BINGO, "gui/text_colors.json"))
                    .getOrNull()
                    ?.inputStream
                    ?.use {
                        @OptIn(ExperimentalSerializationApi::class)
                        json.decodeFromStream<Map<String, String>>(it)
                    }

                this@BingoHudRenderer.colorMap = colorMap ?: emptyMap()
            }

            override fun getFabricId(): Identifier = Identifier(MOD_ID_BINGO, "hud_renderer")
        })
    }

}