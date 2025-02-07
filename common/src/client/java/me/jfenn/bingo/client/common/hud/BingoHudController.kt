package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.*
import me.jfenn.bingo.client.platform.event.IClientEvents
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.KEYBIND_TOGGLE_HUD
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.IServerEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds

internal class BingoHudController(
    private val optionsAccessor: IOptionsAccessor,
    private val configService: ConfigService,
    private val config: BingoConfig,
    private val renderer: BingoHudRenderer,
    private val itemScoredRenderer: BingoMessageRenderer,
    private val readyHudRenderer: ReadyHudRenderer,
    private val hudScreenFactory: BingoHudScreen.Factory,
    private val endScreenFactory: BingoEndScreen.Factory,
    private val state: BingoHudState,
    private val text: TextProvider,
    keyBindingManager: IKeyBindingManager,
    hud: IHudCallback,
    packetEvents: ClientPacketEvents,
    serverEvents: IServerEvents,
    clientEvents: IClientEvents,
) : BingoComponent() {

    private val client = MinecraftClient.getInstance()

    private val openCardKey = keyBindingManager.registerKey(
        KEYBIND_OPEN_CARD,
        GLFW.GLFW_KEY_Y,
        StringKey.FullName.key,
    )

    private val toggleHudKey = keyBindingManager.registerKey(
        KEYBIND_TOGGLE_HUD,
        GLFW.GLFW_KEY_UNKNOWN,
        StringKey.FullName.key,
    )

    private var fireworkRenderer: BingoEndFireworkRenderer? = null

    private val interpolateCardX = Interpolate(0f, 0f)
    private val interpolateCardY = Interpolate(0f, 0f)
    private val interpolateCardScale = Interpolate(0f, 0f)

    private fun onCardHudClosed(
        cardPositions: Map<BingoTeamKey?, Pair<Float, Float>>,
    ) {
        val now = Clock.System.now()
        val lastPosition = cardPositions[getSelectedCard()?.teamKey] ?: return

        interpolateCardX.apply {
            from = lastPosition.first
            duration = 250.milliseconds
            startedAt = now
        }
        interpolateCardY.apply {
            from = lastPosition.second
            duration = 250.milliseconds
            startedAt = now
        }
        interpolateCardScale.apply {
            from = 1f
            duration = 250.milliseconds
            startedAt = now
        }
    }

    private fun getCardPos(): Pair<Float, Float> {
        val client = MinecraftClient.getInstance()

        val cardScale = interpolateCardScale.get(Interpolate.Easing.IN_OUT)
        val cardAlignment = config.client.cardAlignment

        val offsetX = config.client.cardOffsetX * (if (cardAlignment.x > 0) -1 else 1)
        val offsetY = config.client.cardOffsetY * (if (cardAlignment.y > 0) -1 else 1)

        val x = cardAlignment.x * (client.window.scaledWidth - (BingoHudRenderer.CARD_WIDTH * cardScale)) + offsetX
        val y = cardAlignment.y * (client.window.scaledHeight - (BingoHudRenderer.CARD_HEIGHT * cardScale)) + offsetY
        interpolateCardX.to = x
        interpolateCardY.to = y

        return Pair(
            interpolateCardX.get(Interpolate.Easing.IN_OUT),
            interpolateCardY.get(Interpolate.Easing.IN_OUT),
        )
    }

    private fun getSelectedCard(): CardView? {
        val teamKeys = state.views.keys
        if (teamKeys.isEmpty()) return null

        val selectedTeam = state.selectedTeam
        return when {
            // If the selected team is one of the displayed cards, try to find its view state...
            selectedTeam in teamKeys -> state.views[selectedTeam]
            // otherwise, find a different card to display
            else -> state.views.keys.firstOrNull().let { state.views[it] } // firstOrNull() falls back to preview card
        }
    }

    private fun drawHud(drawService: IDrawService) {
        val context = drawService.context
        val textRenderer = MinecraftClient.getInstance().textRenderer

        // If the selected team is one of the displayed cards, try to find its view state...
        val view = getSelectedCard() ?: return
        // Ensure that `selectedTeam` is set to the displayed card, in case they don't match
        state.selectedTeam = view.teamKey

        val cardScale = interpolateCardScale
            .apply { to = config.client.cardScale }
            .get(Interpolate.Easing.IN_OUT)

        val cardAlignment = config.client.cardAlignment
        val (x, y) = getCardPos()

        if (fireworkRenderer?.isDone() == true) {
            fireworkRenderer = null
        } else {
            fireworkRenderer?.render(
                context = context,
                textRenderer = textRenderer,
                centerX = x.toInt() + (BingoHudRenderer.CARD_WIDTH/2),
                centerY = y.toInt() + (BingoHudRenderer.CARD_HEIGHT/2),
                now = state.now,
            )
        }

        val client = MinecraftClient.getInstance()
        val mouse = client.mouse
        val scaleFactor = client.window.scaleFactor
        val mouseX = (mouse.x / scaleFactor).toInt()
        val mouseY = (mouse.y / scaleFactor).toInt()
        val isMouseOver = client.currentScreen is ChatScreen
        renderer.drawCard(
            context,
            drawService,
            view,
            x = x,
            y = y,
            z = config.client.cardOverlap.z,
            cardScale = cardScale,
            // if the client can only see one card, enable team outlines
            // inverse: if the client can see every card, we don't need to show the outlines
            showTeamOutlines = config.client.cardTeamOutlines && (
                !config.client.showMultipleCards || state.views.size <= 1
            ),
            isMouseOver = isMouseOver,
            mouseX = mouseX,
            mouseY = mouseY,
        )

        if (interpolateCardX.isDone()) {
            itemScoredRenderer.drawMessages(
                drawService,
                x = x.toInt(),
                y = y.toInt() + (BingoHudRenderer.CARD_HEIGHT * cardScale * (1 - config.client.cardAlignment.y)).toInt(),
                z = config.client.cardOverlap.z,
                messageScale = config.client.messageScale,
                cardAlignment = config.client.cardAlignment,
            )
        }

        // Attempt to draw additional cards if showMultipleCards=true
        val xDirection = if (cardAlignment.x > 0) -1 else 1
        if (config.client.showMultipleCards) {
            val remainingViews = state.views.values - view

            for ((i, remainingView) in remainingViews.withIndex()) {
                val cardX = x + ((i+1) * xDirection * BingoHudRenderer.CARD_WIDTH * cardScale)
                val cardEndX = x + ((i+2) * xDirection * BingoHudRenderer.CARD_WIDTH * cardScale)
                // if the card would go off-screen, don't bother rendering it...
                if (cardEndX < 0 || cardEndX > context.scaledWindowWidth) continue

                renderer.drawCard(
                    context,
                    drawService,
                    remainingView,
                    x = cardX,
                    y = y,
                    z = config.client.cardOverlap.z,
                    cardScale =  cardScale,
                    showTeamOutlines = false,
                    isMouseOver = isMouseOver,
                    mouseX = mouseX,
                    mouseY = mouseY,
                )
            }
        }

        readyHudRenderer.drawReadyHud(
            drawService = drawService,
            x = context.scaledWindowWidth/2 - 50,
            y = 40,
            width = 100
        )

        val now = state.now
        val tooltipPacket = state.tooltip
        val tooltipText = tooltipPacket?.text
        val tooltipStartedAt = state.tooltipStartedAt ?: Instant.DISTANT_PAST
        if (tooltipText != null) {
            val tooltip = tooltipText // .flatMap { it.wrap(30) }
            val tooltipSize = tooltip.maxOfOrNull { drawService.getTextWidth(it) } ?: 0
            val tooltipX = context.scaledWindowWidth/2 - tooltipSize/2 - 12
            val tooltipY = (context.scaledWindowHeight * 0.5).toInt() + 18 + 12

            val fadeIn = ((now - tooltipStartedAt) / 250.milliseconds).coerceIn(0.0, 1.0)
            val fadeOut = 1.0 - ((now - tooltipPacket.createdAt.plus(100.milliseconds)) / 250.milliseconds).coerceIn(0.0, 1.0)

            drawService.setShaderColor(1f, 1f, 1f, (fadeIn * fadeOut).toFloat())
            context.drawTooltip(textRenderer, tooltip.map { it.value }, tooltipX, tooltipY)
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
        gameOver.views = (state.views.values + gameOver.views)
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
            MinecraftClient.getInstance().setScreen(
                endScreenFactory.create(
                    gameOver,
                    originalCardKey = getSelectedCard()?.teamKey,
                    originalCardPos = getCardPos(),
                    canEscape = false,
                    onClose = { onCardHudClosed(it.getCardPositions()) },
                )
            )
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

    @Suppress("Deprecation")
    private fun onCardUpdate(clientPacket: ClientPacket<CardUpdatePacket>) {
        val packet = clientPacket.packet
        state.views += packet.view.teamKey to packet.view
        updateGameOverViews()
    }

    private fun onReadyUpdate(clientPacket: ClientPacket<ReadyUpdatePacket>) {
        val packet = clientPacket.packet
        state.ready = packet
    }

    private fun onCardDisplayV1(clientPacket: ClientPacket<CardDisplayPacket>) {
        val displays = clientPacket.packet.display
        state.views = displays.mapValues { (key, display) ->
            state.views[key]
                ?.let {
                    // only overwrite teamColor from display, as the rest will come from card update packets
                    it.copy(
                        display = it.display.copy(
                            teamColor = display.teamColor,
                        )
                    )
                }
                ?: CardView(key, display, mutableListOf())
        }

        updateGameOverViews()

        // if there are no teams being displayed (BingoMapController sends when entering PREGAME)
        // reset the HUD state completely
        // (which clears the gameOverPacket state)
        if (displays.isEmpty()) {
            state.reset()
        }
    }

    private fun onCardDisplayV2(clientPacket: ClientPacket<CardDisplayPacket>) {
        val displays = clientPacket.packet.display
        state.views = displays.mapValues { (key, display) ->
            state.views[key]
                ?.copy(display = display)
                ?: CardView(key, display, mutableListOf())
        }

        updateGameOverViews()

        // if there are no teams being displayed (BingoMapController sends when entering PREGAME)
        // reset the HUD state completely
        // (which clears the gameOverPacket state)
        if (displays.isEmpty()) {
            state.reset()
        }
    }

    private fun onCardTiles(clientPacket: ClientPacket<CardTilesPacket>) {
        val packet = clientPacket.packet
        val view = state.views[packet.teamKey] ?: CardView(packet.teamKey, CardDisplay(), mutableListOf())
        val tiles = MutableList(25) {
            val existingTile = view.tiles.getOrNull(it)
            val newTile = packet.tiles[it] ?: existingTile ?: CardTile.EMPTY

            newTile.updatedAt = when {
                // edge case - if almost the entire card is re-rolled, don't show updates
                packet.tiles.size > 20 -> null
                newTile.id != existingTile?.id -> state.now
                else -> existingTile?.updatedAt
            }

            newTile
        }

        state.views += (packet.teamKey to view.copy(tiles = tiles))
        updateGameOverViews()
    }

    init {
        packetEvents.cardDisplayV1.onPacket(::onCardDisplayV1)
        packetEvents.cardDisplayV2.onPacket(::onCardDisplayV2)

        packetEvents.cardUpdateV2.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV3.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV4.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV5.onPacket(::onCardUpdate)
        packetEvents.cardUpdateV6.onPacket(::onCardUpdate)

        packetEvents.cardTilesV1.onPacket(::onCardTiles)

        packetEvents.cardCompletedV1.onPacket {
            if (it.packet.isWinner) {
                fireworkRenderer = BingoEndFireworkRenderer(range = (100 * config.client.cardScale).toInt())
            }
        }

        packetEvents.scoreMessageV1.onPacket(::onScoreMessage)
        packetEvents.scoreMessageV2.onPacket(::onScoreMessage)
        packetEvents.scoreMessageV3.onPacket(::onScoreMessage)

        packetEvents.gameOverV1.onPacket(::onGameOver)
        packetEvents.gameOverV2.onPacket(::onGameOver)
        packetEvents.gameOverV3.onPacket(::onGameOver)
        packetEvents.gameOverV4.onPacket(::onGameOver)
        packetEvents.gameOverV5.onPacket(::onGameOver)
        packetEvents.gameOverV6.onPacket(::onGameOver)
        packetEvents.gameOverV7.onPacket(::onGameOver)

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
            state.now = Clock.System.now()

            // Do not draw the HUD if the F3 or chat overlay is shown
            if (optionsAccessor.isHudHidden())
                return@onRender
            if (config.client.hideOnF3 && optionsAccessor.isDebugEnabled())
                return@onRender
            if (config.client.hideOnChat && MinecraftClient.getInstance().currentScreen is ChatScreen)
                return@onRender
            if (!config.client.enableHud)
                return@onRender

            // Do not draw the HUD if the card screen is open
            if (
                MinecraftClient.getInstance().currentScreen
                    ?.javaClass?.packageName
                    ?.startsWith("me.jfenn.bingo")
                    == true
            ) {
                return@onRender
            }

            drawHud(it.drawService)
        }

        clientEvents.onServerJoin {
            client.execute { state.reset() }
        }

        clientEvents.onServerDisconnect {
            client.execute { state.resetAll() }
        }

        serverEvents.onServerStarted {
            client.execute { state.reset() }
        }

        serverEvents.onServerStopped {
            client.execute { state.resetAll() }
        }

        clientEvents.onInvalidateRenderState {
            client.execute { state.reset() }
        }

        clientEvents.onClientTickEnd {
            if (client.isPaused) return@onClientTickEnd

            if (openCardKey.wasPressed()) {
                if (optionsAccessor.isSneakPressed() && state.ready?.isRunning == true) {
                    packetEvents.readySetV1.send(SetReadyPacket(!(state.ready?.isReady ?: false)))
                } else {
                    if (state.views.isEmpty()) {
                        client.player?.sendMessage(text.string(StringKey.CardNoCards).value, true)
                        return@onClientTickEnd
                    }

                    val gameOver = state.gameOver
                    val hudScreen = if (gameOver != null) {
                        endScreenFactory.create(
                            gameOver = gameOver,
                            originalCardKey = getSelectedCard()?.teamKey,
                            originalCardPos = getCardPos(),
                            canEscape = true,
                            onClose = { onCardHudClosed(it.getCardPositions()) }
                        )
                    } else {
                        hudScreenFactory.create(
                            originalCardKey = getSelectedCard()?.teamKey,
                            originalCardPos = getCardPos(),
                            onClose = { onCardHudClosed(it.getCardPositions()) }
                        )
                    }

                    client.setScreen(hudScreen)
                }
            }

            if (toggleHudKey.wasPressed()) {
                config.client.enableHud = !config.client.enableHud
                configService.writeConfig(config)
            }
        }

        clientEvents.onAssetsReload {
            renderer.loadCardColors(it.resourceManager)
        }
    }
}