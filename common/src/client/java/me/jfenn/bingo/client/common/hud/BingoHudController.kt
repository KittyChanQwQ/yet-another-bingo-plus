package me.jfenn.bingo.client.common.hud

import me.jfenn.bingo.client.api.IDrawServiceFactory
import me.jfenn.bingo.client.api.IOptionsAccessor
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.PACKET_CARD_DISPLAY_S2C
import me.jfenn.bingo.common.PACKET_CARD_UPDATE_S2C
import me.jfenn.bingo.common.PACKET_GAME_OVER_S2C
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.map.CardTeams
import me.jfenn.bingo.common.map.CardView
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.text.GameOverPacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.koin.core.Koin
import org.lwjgl.glfw.GLFW

class BingoHudController(
    private val koin: Koin,
    private val drawServiceFactory: IDrawServiceFactory,
    private val optionsAccessor: IOptionsAccessor,
    private val config: BingoConfig,
    private val renderer: BingoHudRenderer,
    private val state: BingoHudState,
    private val text: TextProvider,
) : BingoComponent() {

    private val openCardKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            KEYBIND_OPEN_CARD,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            StringKey.FullName.key,
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

        renderer.drawCard(
            context,
            drawService,
            view,
            x = x.toInt(),
            y = y.toInt(),
            z = config.client.cardOverlap.z,
            cardScale = cardScale,
        )

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

                renderer.drawCard(
                    context,
                    drawService,
                    remainingView,
                    x = cardX.toInt(),
                    y = y.toInt(),
                    z = config.client.cardOverlap.z,
                    cardScale =  cardScale
                )
            }
        }
    }

    init {
        if (config.supportClientHud) {
            ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_DISPLAY_S2C) { client, handler, buf, response ->
                val display = CardTeams.fromPacketBuf(buf)
                client.execute { state.display = display }
            }

            ClientPlayNetworking.registerGlobalReceiver(PACKET_CARD_UPDATE_S2C) { client, handler, buf, response ->
                val view = CardView.fromPacketBuf(buf)
                client.execute { state.views[view.teamKey] = view }
            }

            ClientPlayNetworking.registerGlobalReceiver(PACKET_GAME_OVER_S2C) { client, handler, buf, response ->
                val packet = GameOverPacket.fromPacketBuf(buf)
                client.execute {
                    state.gameOverPacket = packet
                    client.setScreen(BingoEndScreen(koin, packet, canEscape = false))
                }
            }

            HudRenderCallback.EVENT.register { context, _ ->
                // Do not draw the HUD if the F3 or chat overlay is shown
                if (config.client.hideOnF3 && optionsAccessor.isDebugEnabled())
                    return@register
                if (config.client.hideOnChat && MinecraftClient.getInstance().currentScreen is ChatScreen)
                    return@register

                drawHud(context)
            }

            ClientPlayConnectionEvents.JOIN.register { _, _, client ->
                client.execute { state.reset() }
            }

            ClientPlayConnectionEvents.DISCONNECT.register { _, client ->
                client.execute { state.reset() }
            }

            ServerLifecycleEvents.SERVER_STARTED.register { _ ->
                MinecraftClient.getInstance().execute { state.reset() }
            }

            ServerLifecycleEvents.SERVER_STOPPED.register { _ ->
                MinecraftClient.getInstance().execute { state.reset() }
            }

            InvalidateRenderStateCallback.EVENT.register {
                MinecraftClient.getInstance().execute { state.reset() }
            }

            ClientTickEvents.END_CLIENT_TICK.register { client ->
                if (client.isPaused) return@register

                if (openCardKey.wasPressed()) {
                    val display = state.display?.display
                    if (display.isNullOrEmpty()) {
                        client.player?.sendMessage(text.string(StringKey.CardNoCards), true)
                        return@register
                    }

                    val hudScreen = state.gameOverPacket
                        ?.let { BingoEndScreen(koin, it, canEscape = true) }
                        ?: BingoHudScreen(koin)

                    client.setScreen(hudScreen)
                }
            }
        }
    }
}