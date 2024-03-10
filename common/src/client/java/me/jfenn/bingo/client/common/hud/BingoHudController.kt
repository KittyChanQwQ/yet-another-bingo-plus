package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.client.api.IOptionsAccessor
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.MOD_ID
import me.jfenn.bingo.common.PACKET_CARD_DISPLAY
import me.jfenn.bingo.common.PACKET_CARD_UPDATE
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTeams
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.scope.BingoComponent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

class BingoHudController(
    private val drawServiceFactory: IDrawServiceFactory,
    private val buttonService: IButtonService,
    private val optionsAccessor: IOptionsAccessor,
    private val config: BingoConfig,
) : BingoComponent() {

    private val state = BingoHudState()
    private val renderer = BingoHudRenderer()

    private val openCardKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            KEYBIND_OPEN_CARD,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.$MOD_ID",
        )
    )

    private fun drawHud(context: DrawContext) {
        val drawService = drawServiceFactory.create(context)
        val teamKeys = state.display?.display
        if (teamKeys.isNullOrEmpty()) return

        // If the selected team is one of the displayed cards, try to find its view state...
        val selectedTeam = state.selectedTeam
        val view = when {
            selectedTeam in teamKeys -> state.views[selectedTeam]
            // otherwise, find a different card to display
            else -> state.views[teamKeys.first()]
        } ?: return

        // Ensure that `selectedTeam` is set to the displayed card, in case they don't match
        state.selectedTeam = view.teamKey

        val cardScale = config.client.cardScale
        val cardAlignment = config.client.cardAlignment

        val offsetX = config.client.cardOffsetX * (if (cardAlignment.x > 0) -1 else 1)
        val offsetY = config.client.cardOffsetY * (if (cardAlignment.y > 0) -1 else 1)

        val x = cardAlignment.x * (context.scaledWindowWidth - (BingoHudRenderer.CARD_WIDTH * cardScale)) + offsetX
        val y = cardAlignment.y * (context.scaledWindowHeight - (BingoHudRenderer.CARD_HEIGHT * cardScale)) + offsetY

        renderer.drawCard(context, drawService, view, x.toInt(), y.toInt(), cardScale)

        // Attempt to draw additional cards if showMultipleCards=true
        val xDirection = if (cardAlignment.x > 0) -1 else 1
        if (config.client.showMultipleCards) {
            val remainingViews = (teamKeys - selectedTeam)
                .mapNotNull { state.views[it] }

            for ((i, remainingView) in remainingViews.withIndex()) {
                val cardX = x + ((i+1) * xDirection * BingoHudRenderer.CARD_WIDTH * cardScale)
                val cardEndX = x + ((i+2) * xDirection * BingoHudRenderer.CARD_WIDTH * cardScale)
                // if the card would go off-screen, don't bother rendering it...
                if (cardEndX < 0 || cardEndX > context.scaledWindowWidth) continue

                renderer.drawCard(context, drawService, remainingView, cardX.toInt(), y.toInt(), cardScale)
            }
        }
    }

    init {
        if (config.supportClientHud) {
            ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_DISPLAY) { client, handler, buf, response ->
                val display = CardTeams.fromPacketBuf(buf)
                client.execute { state.display = display }
            }

            ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_UPDATE) { client, handler, buf, response ->
                val view = CardView.fromPacketBuf(buf)
                client.execute { state.views[view.teamKey] = view }
            }

            HudRenderCallback.EVENT.register { context, tickDelta ->
                // Do not draw the HUD if the F3, player list, or chat overlay is shown
                if (optionsAccessor.isDebugEnabled() || optionsAccessor.isPlayerListPressed() || MinecraftClient.getInstance().currentScreen is ChatScreen)
                    return@register

                drawHud(context)
            }

            ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
                client.execute {
                    state.display = null
                    state.views.clear()
                }
            }

            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (client.isPaused) return@register

                if (openCardKey.wasPressed()) {
                    val display = state.display?.display
                    if (display.isNullOrEmpty()) {
                        client.player?.sendMessage(Text.translatable("bingo.card.no_cards"), true)
                        return@register
                    }

                    client.setScreen(BingoHudScreen(drawServiceFactory, buttonService, state))
                }
            }
        }
    }
}