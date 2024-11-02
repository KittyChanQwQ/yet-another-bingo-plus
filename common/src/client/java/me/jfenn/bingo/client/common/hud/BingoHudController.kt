package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.client.api.ClientPacket
import me.jfenn.bingo.client.api.IDrawService
import me.jfenn.bingo.client.api.IHudCallback
import me.jfenn.bingo.client.api.IOptionsAccessor
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.KEYBIND_TOGGLE_HUD
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.CardUpdatePacket
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.wrap
import me.jfenn.bingo.generated.StringKey
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.koin.core.Koin
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds

class BingoHudController(
    private val koin: Koin,
    private val optionsAccessor: IOptionsAccessor,
    private val configService: ConfigService,
    private val config: BingoConfig,
    private val renderer: BingoHudRenderer,
    private val itemScoredRenderer: BingoMessageRenderer,
    private val readyHudRenderer: ReadyHudRenderer,
    private val state: BingoHudState,
    private val text: TextProvider,
    hud: IHudCallback,
    packetEvents: ClientPacketEvents,
) : BingoComponent() {

    private val openCardKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            KEYBIND_OPEN_CARD,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            StringKey.FullName.key,
        )
    )

    private val toggleHudKey = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            KEYBIND_TOGGLE_HUD,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            StringKey.FullName.key,
        )
    )

    private var fireworkRenderer: BingoEndFireworkRenderer? = null

    private fun drawHud(drawService: IDrawService) {
        val teamKeys = state.display?.display
        if (teamKeys.isNullOrEmpty()) return

        val context = drawService.context
        val textRenderer = MinecraftClient.getInstance().textRenderer

        // If the selected team is one of the displayed cards, try to find its view state...
        val selectedTeam = state.selectedTeam
        val view = when {
            selectedTeam in teamKeys -> state.views[selectedTeam]
            // otherwise, find a different card to display
            else -> teamKeys.firstOrNull()?.let { state.views[it] }
        } ?: return

        // Ensure that `selectedTeam` is set to the displayed card, in case they don't match
        state.selectedTeam = view.teamKey

        val cardScale = config.client.cardScale
        val cardAlignment = config.client.cardAlignment

        val offsetX = config.client.cardOffsetX * (if (cardAlignment.x > 0) -1 else 1)
        val offsetY = config.client.cardOffsetY * (if (cardAlignment.y > 0) -1 else 1)

        val x = cardAlignment.x * (context.scaledWindowWidth - (BingoHudRenderer.CARD_WIDTH * cardScale)) + offsetX
        val y = cardAlignment.y * (context.scaledWindowHeight - (BingoHudRenderer.CARD_HEIGHT * cardScale)) + offsetY

        if (fireworkRenderer?.isDone() == true) {
            fireworkRenderer = null
        } else {
            fireworkRenderer?.render(
                context = context,
                textRenderer = textRenderer,
                centerX = x.toInt() + (BingoHudRenderer.CARD_WIDTH/2),
                centerY = y.toInt() + (BingoHudRenderer.CARD_HEIGHT/2)
            )
        }

        renderer.drawCard(
            context,
            drawService,
            view,
            x = x.toInt(),
            y = y.toInt(),
            z = config.client.cardOverlap.z,
            cardScale = cardScale,
            // if the client can only see one card, enable team outlines
            // inverse: if the client can see every card, we don't need to show the outlines
            showTeamOutlines = config.client.cardTeamOutlines && (
                !config.client.showMultipleCards || state.display?.display?.size?.let { it <= 1 } == true
            ),
        )

        itemScoredRenderer.drawMessages(
            drawService,
            x = x.toInt(),
            y = y.toInt() + (BingoHudRenderer.CARD_HEIGHT * cardScale * (1 - config.client.cardAlignment.y)).toInt(),
            z = config.client.cardOverlap.z,
            messageScale = config.client.messageScale,
            cardAlignment = config.client.cardAlignment,
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
                    cardScale =  cardScale,
                    showTeamOutlines = false
                )
            }
        }

        readyHudRenderer.drawReadyHud(
            drawService = drawService,
            x = context.scaledWindowWidth/2 - 50,
            y = 40,
            width = 100
        )

        val now = Clock.System.now()
        val tooltipPacket = state.tooltip
        val tooltipText = tooltipPacket?.text
        val tooltipStartedAt = state.tooltipStartedAt ?: Instant.DISTANT_PAST
        if (tooltipText != null) {
            val tooltip = tooltipText.flatMap { it.wrap(30) }
            val tooltipSize = tooltip.maxOfOrNull { textRenderer.getWidth(it) } ?: 0
            val tooltipX = context.scaledWindowWidth/2 - tooltipSize/2 - 12
            val tooltipY = (context.scaledWindowHeight * 0.5).toInt() + 18 + 12

            val fadeIn = ((now - tooltipStartedAt) / 250.milliseconds).coerceIn(0.0, 1.0)
            val fadeOut = 1.0 - ((now - tooltipPacket.createdAt.plus(100.milliseconds)) / 250.milliseconds).coerceIn(0.0, 1.0)

            drawService.setShaderColor(1f, 1f, 1f, (fadeIn * fadeOut).toFloat())
            context.drawTooltip(textRenderer, tooltip, tooltipX, tooltipY)
            drawService.setShaderColor(1f, 1f, 1f, 1f)

            if (fadeOut <= 0.0) {
                state.tooltip = null
                state.tooltipStartedAt = null
            }
        }
    }

    private fun updateGameOverViews() {
        val gameOver = state.gameOver ?: return

        // Add any new views to the gameOver screen, but do not take away previously-seen views
        // - this is a compatibility fix for previous server versions
        //   that do not ensure all cards are sent before the game over screen
        gameOver.views = (
                (state.display?.display ?: emptyList())
                    .mapNotNull { state.views[it] }
                        + gameOver.views
                )
            // do not show the preview card on the Game Over screen
            .filter { it.teamKey != null }
            // prevent any duplicate cards (uses newly-sent cards first)
            .distinctBy { it.teamKey?.id }
    }

    private fun onGameOver(clientPacket: ClientPacket<GameOverPacket>) {
        val (packet) = clientPacket
        val gameOver = state.gameOver ?: BingoHudState.GameOver(packet, emptyList())
        state.gameOver = gameOver
        updateGameOverViews()

        if (!packet.isUpdate) {
            // when the game first ends, open the end screen
            MinecraftClient.getInstance().setScreen(BingoEndScreen(koin, gameOver, canEscape = false))
        }
    }

    private fun onScoreMessage(packet: ClientPacket<ScoreMessagePacket>) {
        if (!packet.packet.isViewerOnTeam && !config.client.messageFromOtherTeams)
            return

        val scoreMessage = BingoHudState.ScoreMessage(
            createdAt = Clock.System.now(),
            packet = packet.packet,
        )

        state.messages.add(scoreMessage)
    }

    private fun onCardUpdate(clientPacket: ClientPacket<CardUpdatePacket>) {
        val packet = clientPacket.packet
        state.views[packet.view.teamKey] = packet.view
        updateGameOverViews()
    }

    private fun onReadyUpdate(clientPacket: ClientPacket<ReadyUpdatePacket>) {
        val packet = clientPacket.packet
        state.ready = packet
    }

    init {
        packetEvents.cardDisplayV1.onPacket {
            state.display = it.packet
            updateGameOverViews()

            // if there are no teams being displayed (BingoMapController sends when entering PREGAME)
            // reset the HUD state completely
            // (which clears the gameOverPacket state)
            if (it.packet.display.isEmpty()) {
                state.reset()
            }
        }

        packetEvents.cardUpdateV1.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV2.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV3.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV4.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV5.onPacket(::onCardUpdate)

        packetEvents.cardCompletedV1.onPacket {
            if (it.packet.isWinner) {
                fireworkRenderer = BingoEndFireworkRenderer(range = (100 * config.client.cardScale).toInt())
            }
        }

        packetEvents.scoreMessageV1.onPacket(::onScoreMessage)
        packetEvents.scoreMessageV2.onPacket(::onScoreMessage)

        packetEvents.gameOverV1.onPacket(::onGameOver)
        packetEvents.gameOverV2.onPacket(::onGameOver)
        packetEvents.gameOverV3.onPacket(::onGameOver)
        packetEvents.gameOverV4.onPacket(::onGameOver)
        packetEvents.gameOverV5.onPacket(::onGameOver)
        packetEvents.gameOverV6.onPacket(::onGameOver)

        packetEvents.readyUpdateV1.onPacket(::onReadyUpdate)
        packetEvents.readyUpdateV2.onPacket(::onReadyUpdate)
        packetEvents.readyUpdateV3.onPacket(::onReadyUpdate)

        packetEvents.tooltipV1.onPacket {
            state.tooltip = it.packet
            if (state.tooltipStartedAt == null) {
                state.tooltipStartedAt = it.packet.createdAt
            }
        }

        hud.onRender {
            // Do not draw the HUD if the F3 or chat overlay is shown
            if (optionsAccessor.isHudHidden())
                return@onRender
            if (config.client.hideOnF3 && optionsAccessor.isDebugEnabled())
                return@onRender
            if (config.client.hideOnChat && MinecraftClient.getInstance().currentScreen is ChatScreen)
                return@onRender
            if (!config.client.enableHud)
                return@onRender

            drawHud(it.drawService)
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, client ->
            client.execute { state.reset() }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, client ->
            client.execute { state.resetAll() }
        }

        ServerLifecycleEvents.SERVER_STARTED.register { _ ->
            MinecraftClient.getInstance().execute { state.reset() }
        }

        ServerLifecycleEvents.SERVER_STOPPED.register { _ ->
            MinecraftClient.getInstance().execute { state.resetAll() }
        }

        InvalidateRenderStateCallback.EVENT.register {
            MinecraftClient.getInstance().execute { state.reset() }
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.isPaused) return@register

            if (openCardKey.wasPressed()) {
                if (optionsAccessor.isSneakPressed() && state.ready?.isRunning == true) {
                    packetEvents.readySetV1.send(SetReadyPacket(!(state.ready?.isReady ?: false)))
                } else {
                    val display = state.display?.display
                    if (display.isNullOrEmpty()) {
                        client.player?.sendMessage(text.string(StringKey.CardNoCards), true)
                        return@register
                    }

                    val hudScreen = state.gameOver
                        ?.let { BingoEndScreen(koin, it, canEscape = true) }
                        ?: BingoHudScreen(koin)

                    client.setScreen(hudScreen)
                }
            }

            if (toggleHudKey.wasPressed()) {
                config.client.enableHud = !config.client.enableHud
                configService.writeConfig(config)
            }
        }
    }
}