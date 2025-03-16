package me.jfenn.bingo.client.common.hud

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.common.event.ClientConfigChangedEvent
import me.jfenn.bingo.client.common.event.HudStateChangedEvent
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardManager
import me.jfenn.bingo.client.common.hud.card.ClientCardRenderer
import me.jfenn.bingo.client.common.hud.screen.BingoEndScreen
import me.jfenn.bingo.client.common.hud.screen.BingoHudScreen
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.state.ClientCard
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.ClientPacket
import me.jfenn.bingo.client.platform.IKeyBindingManager
import me.jfenn.bingo.client.platform.IOptionsAccessor
import me.jfenn.bingo.client.platform.event.model.*
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.KEYBIND_TOGGLE_HUD
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.GameMessagePacket
import me.jfenn.bingo.common.scoring.ScoreMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ServerEvent
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

internal class BingoHudController(
    private val optionsAccessor: IOptionsAccessor,
    private val configService: ConfigService,
    private val config: BingoConfig,
    private val renderer: ClientCardRenderer,
    private val clientCardManager: ClientCardManager,
    private val itemScoredRenderer: BingoMessageRenderer,
    private val readyHudRenderer: ReadyHudRenderer,
    private val hudScreenFactory: BingoHudScreen.Factory,
    private val endScreenFactory: BingoEndScreen.Factory,
    private val state: BingoHudState,
    private val text: TextProvider,
    keyBindingManager: IKeyBindingManager,
    packetEvents: ClientPacketEvents,
    private val eventBus: IEventBus,
) : BingoComponent() {

    private val client = MinecraftClient.getInstance()

    private var openCardKeyAlreadyPressed: Boolean = false
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

        val x = cardAlignment.x * (client.window.scaledWidth - (ClientCardBufferRenderer.CARD_WIDTH * cardScale)) + offsetX
        val y = cardAlignment.y * (client.window.scaledHeight - (ClientCardBufferRenderer.CARD_HEIGHT * cardScale)) + offsetY
        interpolateCardX.to = x
        interpolateCardY.to = y

        return Pair(
            interpolateCardX.get(Interpolate.Easing.IN_OUT),
            interpolateCardY.get(Interpolate.Easing.IN_OUT),
        )
    }

    private fun getSelectedCard(): ClientCard? {
        val teamKeys = state.cards.keys
        if (teamKeys.isEmpty()) return null

        val selectedTeam = state.selectedTeam
        return when {
            // If the selected team is one of the displayed cards, try to find its view state...
            selectedTeam in teamKeys -> state.cards[selectedTeam]
            // otherwise, find a different card to display
            else -> state.cards.keys.firstOrNull().let { state.cards[it] } // firstOrNull() falls back to preview card
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
                drawService = drawService,
                centerX = x.toInt() + (ClientCardBufferRenderer.CARD_WIDTH/2),
                centerY = y.toInt() + (ClientCardBufferRenderer.CARD_HEIGHT/2),
                now = state.now,
            )
        }

        val client = MinecraftClient.getInstance()
        val mouse = client.mouse
        val scaleFactor = client.window.scaleFactor
        val mouseX = (mouse.x / scaleFactor).toInt()
        val mouseY = (mouse.y / scaleFactor).toInt()
        val isMouseOver = client.currentScreen is ChatScreen

        renderer.draw(
            context,
            drawService,
            view,
            x = x,
            y = y,
            z = config.client.cardOverlap.z,
            cardScale = cardScale,
            isMouseOver = isMouseOver,
            mouseX = mouseX,
            mouseY = mouseY,
        )

        if (interpolateCardX.isDone()) {
            itemScoredRenderer.drawMessages(
                drawService,
                x = x.toInt(),
                y = y.toInt() + (ClientCardBufferRenderer.CARD_HEIGHT * cardScale * (1 - config.client.cardAlignment.y)).toInt(),
                z = config.client.cardOverlap.z,
                messageScale = config.client.messageScale,
                cardAlignment = config.client.cardAlignment,
                width = client.window.width/2
            )
        }

        // Attempt to draw additional cards if showMultipleCards=true
        val xDirection = if (cardAlignment.x > 0) -1 else 1
        if (config.client.showMultipleCards) {
            val remainingViews = state.cards.values - view

            for ((i, remainingView) in remainingViews.withIndex()) {
                val cardX = x + ((i+1) * xDirection * ClientCardBufferRenderer.CARD_WIDTH * cardScale)
                val cardEndX = x + ((i+2) * xDirection * ClientCardBufferRenderer.CARD_WIDTH * cardScale)
                // if the card would go off-screen, don't bother rendering it...
                if (cardEndX < 0 || cardEndX > context.scaledWindowWidth) continue

                renderer.draw(
                    context,
                    drawService,
                    remainingView,
                    x = cardX,
                    y = y,
                    z = config.client.cardOverlap.z,
                    cardScale =  cardScale,
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
        val tooltipText = tooltipPacket?.text?.let { tooltipText ->
            // Get or create clientWrappedText
            tooltipPacket.clientWrappedText
                ?: tooltipText.flatMap { line -> drawService.font.wrapLines(line, 200) }
                    .also { tooltipPacket.clientWrappedText = it }
        }
        val tooltipStartedAt = state.tooltipStartedAt ?: Instant.DISTANT_PAST
        if (tooltipText != null && MinecraftClient.getInstance().currentScreen == null) {
            val tooltipSize = tooltipText.maxOfOrNull { drawService.font.getTextWidth(it) } ?: 0
            val tooltipX = context.scaledWindowWidth/2 - tooltipSize/2 - 12
            val tooltipY = (context.scaledWindowHeight * 0.5).toInt() + 18 + 12

            val fadeIn = ((now - tooltipStartedAt) / 250.milliseconds).coerceIn(0.0, 1.0)
            val fadeOut = 1.0 - ((now - tooltipPacket.createdAt.plus(100.milliseconds)) / 250.milliseconds).coerceIn(0.0, 1.0)

            drawService.setShaderColor(1f, 1f, 1f, (fadeIn * fadeOut).toFloat())
            context.drawTooltip(textRenderer, tooltipText.map { it.value }, tooltipX, tooltipY)
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
        gameOver.cards = (state.cards.values + gameOver.cards)
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

    private fun onMessagePacket(packet: GameMessagePacket) {
        val scoreMessage = BingoHudState.ScoreMessage(
            createdAt = Clock.System.now(),
            packet = packet,
        )

        if (!packet.isUpdate) {
            state.messages.add(scoreMessage)
        }

        state.pastMessages.removeIf { it.packet.id == packet.id }
        state.pastMessages.add(scoreMessage)
        state.pastMessages.sortBy { it.packet.timeElapsed }
        // Notify the screens to update, if open
        eventBus.emit(HudStateChangedEvent, Unit)
    }

    private fun onScoreMessage(packet: ClientPacket<ScoreMessagePacket>) {
        if (packet.packet.isViewerOnTeam || config.client.messageFromOtherTeams) {
            onMessagePacket(packet.packet.toGameMessagePacket())
        }
    }

    private fun onGameMessage(packet: ClientPacket<GameMessagePacket>) {
        onMessagePacket(packet.packet)
    }

    @Suppress("Deprecation")
    private fun onCardUpdate(clientPacket: ClientPacket<CardUpdatePacket>) {
        val packet = clientPacket.packet
        val clientCard = state.cards.getOrPut(packet.view.teamKey) {
            clientCardManager.newCard(packet.view)
        }
        clientCard.view = packet.view

        updateGameOverViews()
    }

    private fun onReadyUpdate(clientPacket: ClientPacket<ReadyUpdatePacket>) {
        val packet = clientPacket.packet
        val isStateChanged = state.ready?.canSendReady != packet.canSendReady ||
                state.ready?.isReady != packet.isReady ||
                state.ready?.isRunning != packet.isRunning

        state.ready = packet
        if (isStateChanged) {
            eventBus.emit(HudStateChangedEvent, Unit)
        }
    }

    private fun onCardDisplay(displays: Map<BingoTeamKey?, CardDisplay>) {
        // Remove cards that are no longer displayed
        val cardsIterator = state.cards.iterator()
        for ((key, card) in cardsIterator) {
            if (key !in displays) {
                card.close()
                cardsIterator.remove()
            }
        }

        for ((key, display) in displays) {
            val clientCard = state.cards.getOrPut(key) {
                clientCardManager.newCard(CardView(key, display, mutableListOf()))
            }
            clientCard.view = clientCard.view.copy(display = display)
            clientCard.colors = state.cardColors.getTeamColors(key, display.teamColor)
        }

        displays.keys.forEach {
            clientCardManager.markDirty(it)
        }

        updateGameOverViews()

        // if there are no teams being displayed (BingoMapController sends when entering PREGAME)
        // reset the HUD state completely
        // (which clears the gameOverPacket state)
        if (displays.isEmpty()) {
            state.reset()
        }
    }

    private fun onCardDisplayV1(clientPacket: ClientPacket<CardDisplayPacket>) {
        onCardDisplay(
            clientPacket.packet.display.mapValues { (key, display) ->
                state.cards[key]?.display
                    // only overwrite teamColor from display, as the rest will come from card update packets
                    ?.copy(teamColor = display.teamColor)
                    ?: display
            }
        )
    }

    private fun onCardDisplayV2(clientPacket: ClientPacket<CardDisplayPacket>) {
        onCardDisplay(clientPacket.packet.display)
    }

    private fun scheduleCardUpdates(teamKey: BingoTeamKey?) {
        // Schedule card redraws that occur when a tile is flashing on the card
        for (delay in 500L..4500L step 500L) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, client)
                .execute {
                    clientCardManager.markDirty(teamKey)
                }
        }
    }

    private fun onCardTiles(clientPacket: ClientPacket<CardTilesPacket>) {
        val packet = clientPacket.packet
        val clientCard = state.cards.getOrPut(packet.teamKey) {
            clientCardManager.newCard(CardView(packet.teamKey, CardDisplay(), mutableListOf()))
        }
        val tiles = MutableList(25) {
            val existingTile = clientCard.tiles.getOrNull(it)
            val newTile = packet.tiles[it] ?: existingTile ?: CardTile.EMPTY

            newTile.updatedAt = when {
                // edge case - if almost the entire card is re-rolled, don't show updates
                packet.tiles.size > 20 -> null
                newTile.id != existingTile?.id -> state.now
                else -> existingTile?.updatedAt
            }

            newTile
        }

        if (tiles.any { it.updatedAt == state.now })
            scheduleCardUpdates(packet.teamKey)

        clientCard.view = clientCard.view.copy(tiles = tiles)
        updateGameOverViews()
        clientCardManager.markDirty(packet.teamKey)
    }

    private fun onCardShuffled(clientPacket: ClientPacket<CardShuffledPacket>) {
        val packet = clientPacket.packet
        val clientCard = state.cards[packet.teamKey]
        if (clientCard != null && (state.now - clientCard.shuffledAt) > ClientCardBufferRenderer.SHUFFLE_DURATION) {
            clientCard.shuffledAt = state.now
            clientCard.shufflePositions = clientCard.shufflePositions.shuffled()
        }
    }

    private val cardColorsId = Identifier.of(MOD_ID_BINGO, "gui/card_colors.json")!!

    private fun loadCardColors(manager: ResourceManager) {
        state.cardColors = manager.getResource(cardColorsId)
            .getOrNull()
            ?.inputStream
            ?.use {
                @OptIn(ExperimentalSerializationApi::class)
                json.decodeFromStream<BingoCardColors>(it)
            }
            ?: BingoCardColors()

        for (card in state.cards.values) {
            card.colors = state.cardColors.getTeamColors(card.teamKey, card.display.teamColor)
            card.display.clientTeamName = null
            card.tiles.forEach { tile ->
                tile.clientTooltip = null
            }
            clientCardManager.markDirty(card.teamKey)
        }
    }

    init {
        eventBus.register(packetEvents.cardDisplayV1, ::onCardDisplayV1)
        eventBus.register(packetEvents.cardDisplayV1, ::onCardDisplayV1)
        eventBus.register(packetEvents.cardDisplayV2, ::onCardDisplayV2)

        eventBus.register(packetEvents.cardUpdateV2, ::onCardUpdate)
        eventBus.register(packetEvents.cardUpdateV3, ::onCardUpdate)
        eventBus.register(packetEvents.cardUpdateV4, ::onCardUpdate)
        eventBus.register(packetEvents.cardUpdateV5, ::onCardUpdate)
        eventBus.register(packetEvents.cardUpdateV6, ::onCardUpdate)

        eventBus.register(packetEvents.cardTilesV1, ::onCardTiles)
        eventBus.register(packetEvents.cardShuffledV1, ::onCardShuffled)

        eventBus.register(packetEvents.cardCompletedV1) {
            if (it.packet.isWinner) {
                fireworkRenderer = BingoEndFireworkRenderer(text, range = (100 * config.client.cardScale).toInt())
            }
        }

        eventBus.register(packetEvents.scoreMessageV1, ::onScoreMessage)
        eventBus.register(packetEvents.scoreMessageV2, ::onScoreMessage)
        eventBus.register(packetEvents.scoreMessageV3, ::onScoreMessage)

        eventBus.register(packetEvents.gameStatusV1) {
            state.gameStatus = it.packet
            eventBus.emit(HudStateChangedEvent, Unit)
        }
        eventBus.register(packetEvents.gameMessageV1, ::onGameMessage)
        eventBus.register(packetEvents.gameMessageClearV1) {
            state.messages.clear()
            state.pastMessages.clear()
            eventBus.emit(HudStateChangedEvent, Unit)
        }

        eventBus.register(packetEvents.gameOverV1, ::onGameOver)
        eventBus.register(packetEvents.gameOverV2, ::onGameOver)
        eventBus.register(packetEvents.gameOverV3, ::onGameOver)
        eventBus.register(packetEvents.gameOverV4, ::onGameOver)
        eventBus.register(packetEvents.gameOverV5, ::onGameOver)
        eventBus.register(packetEvents.gameOverV6, ::onGameOver)
        eventBus.register(packetEvents.gameOverV7, ::onGameOver)
        eventBus.register(packetEvents.gameOverV8, ::onGameOver)

        eventBus.register(packetEvents.readyUpdateV1, ::onReadyUpdate)
        eventBus.register(packetEvents.readyUpdateV2, ::onReadyUpdate)
        eventBus.register(packetEvents.readyUpdateV3, ::onReadyUpdate)

        eventBus.register(packetEvents.tooltipV1) {
            state.tooltip = it.packet
            if (state.tooltipStartedAt == null) {
                state.tooltipStartedAt = it.packet.createdAt
            }
        }

        eventBus.register(HudRenderEvent) {
            state.now = Clock.System.now()

            // TODO: move to resize event?
            state.cards.values.forEach { card ->
                clientCardManager.resizeCard(card)
                clientCardManager.resizeCard(card.guiCard, 1f)
            }

            clientCardManager.updateCards()
        }

        eventBus.register(HudRenderEvent) {
            // Do not draw the HUD if the F3 or chat overlay is shown
            if (optionsAccessor.isHudHidden())
                return@register
            if (config.client.hideOnF3 && optionsAccessor.isDebugEnabled())
                return@register
            if (config.client.hideOnChat && MinecraftClient.getInstance().currentScreen is ChatScreen)
                return@register
            if (!config.client.enableHud)
                return@register

            // Do not draw the HUD if the card screen is open
            if (
                MinecraftClient.getInstance().currentScreen
                    ?.javaClass?.packageName
                    ?.startsWith("me.jfenn.bingo")
                    == true
            ) {
                return@register
            }

            drawHud(it.drawService)
        }

        eventBus.register(ClientServerEvent.Join) {
            client.execute { state.reset() }
        }

        eventBus.register(ClientServerEvent.Disconnect) {
            client.execute { state.resetAll() }
        }

        eventBus.register(ServerEvent.Started) {
            client.execute { state.reset() }
        }

        eventBus.register(ServerEvent.Stopped) {
            client.execute { state.resetAll() }
        }

        eventBus.register(InvalidateRenderStateEvent) {
            client.execute { state.reset() }
        }

        eventBus.register(ClientTickEvent.End) {
            if (client.isPaused) return@register

            var openCardKeyWasPressed = false
            while (openCardKey.wasPressed()) openCardKeyWasPressed = true

            if (openCardKeyWasPressed && !openCardKeyAlreadyPressed) {
                if (state.ready?.canSendReady == true && optionsAccessor.isSneakPressed()) {
                    if (state.ready?.isRunning == true) {
                        packetEvents.readySetV1.send(SetReadyPacket(!(state.ready?.isReady ?: false)))
                    }
                } else {
                    if (state.cards.isEmpty()) {
                        client.player?.sendMessage(text.string(StringKey.CardNoCards).value, true)
                        return@register
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

                // Prevent holding down the "Ready" keybind from toggling the ready packet multiple times
                openCardKeyAlreadyPressed = true
            } else if (!openCardKey.isPressed()) {
                openCardKeyAlreadyPressed = false
            }

            if (toggleHudKey.wasPressed()) {
                config.client.enableHud = !config.client.enableHud
                configService.writeConfig(config)
            }
        }

        eventBus.register(ClientReloadEvent) {
            loadCardColors(it.resourceManager)
        }

        eventBus.register(ClientConfigChangedEvent) {
            state.cards.values.forEach {
                clientCardManager.markDirty(it.teamKey)
            }
        }
    }
}