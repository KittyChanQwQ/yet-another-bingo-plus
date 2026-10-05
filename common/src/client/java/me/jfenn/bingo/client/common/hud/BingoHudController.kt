package me.jfenn.bingo.client.common.hud

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import me.jfenn.bingo.client.common.event.ClientConfigChangedEvent
import me.jfenn.bingo.client.common.event.HudStateChangedEvent
import me.jfenn.bingo.client.common.hud.card.ClientCardBufferRenderer
import me.jfenn.bingo.client.common.hud.card.ClientCardManager
import me.jfenn.bingo.client.common.hud.card.ClientCardRenderer
import me.jfenn.bingo.client.common.hud.screen.BingoHudScreen
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.state.ClientCard
import me.jfenn.bingo.client.platform.*
import me.jfenn.bingo.client.platform.event.model.*
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.KEYBIND_TOGGLE_HUD
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.CardAlignment
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.game.GameOverPacket
import me.jfenn.bingo.common.map.*
import me.jfenn.bingo.common.ready.ReadyUpdatePacket
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scoring.GameMessagePacket
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.json
import me.jfenn.bingo.common.utils.minus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.ServerEvent
import me.jfenn.bingo.platform.resource.IResourceManager
import net.minecraft.client.gui.screens.ChatScreen
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class BingoHudController(
    private val optionsAccessor: IOptionsAccessor,
    private val configService: ConfigService,
    private val config: BingoConfig,
    private val renderer: ClientCardRenderer,
    private val clientCardManager: ClientCardManager,
    private val itemScoredRenderer: BingoMessageRenderer,
    private val readyHudRenderer: ReadyHudRenderer,
    private val tooltipHudRenderer: TooltipHudRenderer,
    private val cardScreenFactory: BingoHudScreen.Factory,
    private val state: BingoHudState,
    private val text: TextProvider,
    keyBindingManager: IKeyBindingManager,
    packetEvents: ClientPacketEvents,
    private val eventBus: IEventBus,
    private val client: IClient,
) : BingoComponent() {

    private var cardHudWasClicked: Boolean = false
    private var openCardKeyAlreadyPressed: Boolean = false
    private val openCardKey = keyBindingManager.registerKey(
        KEYBIND_OPEN_CARD,
        KeyCode.KEY_Y,
        StringKey.FullName.key,
    )

    private val toggleHudKey = keyBindingManager.registerKey(
        KEYBIND_TOGGLE_HUD,
        KeyCode.UNKNOWN,
        StringKey.FullName.key,
    )

    private var fireworkRenderer: BingoEndFireworkRenderer? = null

    private var scaledWindowWidth: Int = 0
    private var scaledWindowHeight: Int = 0

    private fun getCardPos(): Pair<Float, Float> {
        val cardScale = config.client.cardScale
        val cardAlignment = config.client.cardAlignment

        val offsetX = config.client.cardOffsetX * (if (cardAlignment.x > 0) -1 else 1)
        val offsetY = config.client.cardOffsetY * (if (cardAlignment.y > 0) -1 else 1)

        val x = cardAlignment.x * (scaledWindowWidth - (ClientCardBufferRenderer.CARD_WIDTH * cardScale)) + offsetX
        val y = cardAlignment.y * (scaledWindowHeight - (ClientCardBufferRenderer.CARD_HEIGHT * cardScale)) + offsetY

        return Pair(x, y)
    }

    fun isCardMouseOver(): Boolean {
        val (x, y) = getCardPos()
        val (mouseX, mouseY) = client.mouse.let { Pair(it.x, it.y) }
        return client.screen is ChatScreen &&
                mouseX.toFloat() in x..(x + ClientCardBufferRenderer.CARD_WIDTH*config.client.cardScale) &&
                mouseY.toFloat() in y..(y + ClientCardBufferRenderer.CARD_HEIGHT*config.client.cardScale)
    }

    fun isCardFocused(): Boolean {
        return config.client.cardAlignment != CardAlignment.BOTTOM_LEFT && isCardMouseOver()
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

    private fun drawHudCards(drawService: IDrawService) {
        // If the selected team is one of the displayed cards, try to find its view state...
        val view = getSelectedCard() ?: return
        // Ensure that `selectedTeam` is set to the displayed card, in case they don't match
        state.selectedTeam = view.teamKey

        val cardScale = config.client.cardScale
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

        val (mouseX, mouseY) = drawService.mouse.let { Pair(it.x, it.y) }

        renderer.draw(
            drawService,
            view,
            x = x,
            y = y,
            z = config.client.cardOverlap.z,
            cardScale = cardScale,
            isMouseOver = isCardMouseOver(),
            isFocused = isCardFocused(),
            mouseX = mouseX,
            mouseY = mouseY,
        )

        itemScoredRenderer.drawMessages(
            drawService,
            x = x.toInt(),
            y = y.toInt() + (ClientCardBufferRenderer.CARD_HEIGHT * cardScale * (1 - config.client.cardAlignment.y)).toInt(),
            z = config.client.cardOverlap.z,
            messageScale = config.client.messageScale,
            cardAlignment = config.client.cardAlignment,
        )

        // Attempt to draw additional cards if showMultipleCards=true
        val xDirection = if (cardAlignment.x > 0) -1 else 1
        if (config.client.showMultipleCards) {
            val remainingViews = state.cards.values - view

            for ((i, remainingView) in remainingViews.withIndex()) {
                val cardX = x + ((i+1) * xDirection * ClientCardBufferRenderer.CARD_WIDTH * cardScale)
                val cardEndX = x + ((i+2) * xDirection * ClientCardBufferRenderer.CARD_WIDTH * cardScale)
                // if the card would go off-screen, don't bother rendering it...
                if (cardEndX < 0 || cardEndX > drawService.window.scaledWindowWidth) continue

                renderer.draw(
                    drawService,
                    remainingView,
                    x = cardX,
                    y = y,
                    z = config.client.cardOverlap.z,
                    cardScale = cardScale,
                )
            }
        }
    }

    private fun drawHudInfo(drawService: IDrawService) {
        readyHudRenderer.drawReadyHud(
            drawService = drawService,
            x = drawService.window.scaledWindowWidth/2 - 50,
            y = 40,
            width = 100
        )

        tooltipHudRenderer.drawTooltip(drawService)
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
            client.screen = cardScreenFactory.create(
                gameOver,
                originalCardKey = getSelectedCard()?.teamKey,
                originalCardPos = getCardPos(),
                canEscape = false,
            )
        }
    }

    private fun onMessagePacket(packet: GameMessagePacket) {
        val scoreMessage = BingoHudState.ScoreMessage(
            createdAt = Instant.now(),
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

    private fun onGameMessage(packet: ClientPacket<GameMessagePacket>) {
        onMessagePacket(packet.packet)
    }

    private fun onReadyUpdate(clientPacket: ClientPacket<ReadyUpdatePacket>) {
        val packet = clientPacket.packet
        val isStateChanged = state.ready?.canSendReady != packet.canSendReady ||
                state.ready?.isReady != packet.isReady ||
                state.ready?.isRunning != packet.isRunning ||
                state.ready?.voteOptions != packet.voteOptions ||
                state.ready?.title != packet.title ||
                state.ready?.subtitle != packet.subtitle

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
                // If the gameOver screen is still showing the card, don't close it!
                if (state.gameOver?.cards?.contains(card) != true) {
                    card.close()
                }
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
        if (displays.isEmpty() && state.gameStatus.isDefaultInstance) {
            state.reset()
        }
    }

    private fun onCardDisplayV2(clientPacket: ClientPacket<CardDisplayPacket>) {
        onCardDisplay(clientPacket.packet.display)
    }

    private fun scheduleCardUpdates(teamKey: BingoTeamKey?) {
        // Schedule card redraws that occur when a tile is flashing on the card
        for (delay in 500L..4500L step 500L) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, client.executor)
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
                !packet.shouldNotify -> null
                newTile.id != existingTile?.id -> state.now
                else -> existingTile?.updatedAt
            }

            newTile
        }

        if (tiles.any { it.updatedAt as Any? == state.now })
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

    private fun openCardScreen(originalCardKey: BingoTeamKey?) {
        val gameOver = state.gameOver
        val hudScreen = if (gameOver != null || state.cards.isNotEmpty()) {
            cardScreenFactory.create(
                gameOver = gameOver,
                originalCardKey = originalCardKey,
                originalCardPos = getCardPos(),
                canEscape = true,
            )
        } else {
            client.player.sendHotbarMessage(text.string(StringKey.CardNoCards))
            return
        }

        client.screen = hudScreen
    }

    private val cardColorsId = Identifier.of(MOD_ID_BINGO, "gui/card_colors.json")

    private fun loadCardColors(manager: IResourceManager) {
        state.cardColors = manager.getResource(cardColorsId)
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
        eventBus.register(packetEvents.cardResetV1) {
            state.reset()
        }

        eventBus.register(packetEvents.cardDisplayV1, ::onCardDisplayV2)

        eventBus.register(packetEvents.cardTilesV1, ::onCardTiles)
        eventBus.register(packetEvents.cardShuffledV1, ::onCardShuffled)

        eventBus.register(packetEvents.cardCompletedV1) {
            if (it.packet.isWinner) {
                fireworkRenderer = BingoEndFireworkRenderer(text, range = (100 * config.client.cardScale).toInt())
            }
        }

        eventBus.register(packetEvents.cardOpenV1) {
            openCardScreen(it.packet.teamKey)
        }

        eventBus.register(packetEvents.gameStatusV1) {
            state.gameStatus = it.packet
            eventBus.emit(HudStateChangedEvent, Unit)
        }
        eventBus.register(packetEvents.gameMessageV1, ::onGameMessage)
        eventBus.register(packetEvents.gameMessageClearV1) {
            state.clearMessages()
            eventBus.emit(HudStateChangedEvent, Unit)
        }

        eventBus.register(packetEvents.gameOverV2, ::onGameOver)
        eventBus.register(packetEvents.gameOverV1, ::onGameOver)

        eventBus.register(packetEvents.readyUpdateV1, ::onReadyUpdate)
        eventBus.register(packetEvents.readyUpdateV2, ::onReadyUpdate)

        eventBus.register(packetEvents.tooltipV1) {
            state.tooltipsByEntity[it.packet.entityId] = it.packet
        }

        eventBus.register(HudRenderEvent) {
            state.now = Instant.now()
            scaledWindowWidth = it.drawService.window.scaledWindowWidth
            scaledWindowHeight = it.drawService.window.scaledWindowHeight

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
            val isDebugEnabled = optionsAccessor.isDebugEnabled()
            val isChatOpen = client.screen is ChatScreen
            val isPlayerListOpen = optionsAccessor.isPlayerListPressed()

            // Do not draw the HUD if the card screen is open
            if (
                client.screen
                    ?.javaClass?.packageName
                    ?.startsWith("me.jfenn.bingo")
                    == true
            ) {
                return@register
            }

            if (
                config.client.enableHud &&
                !(config.client.hideOnF3 && isDebugEnabled) &&
                !(config.client.hideOnChat && isChatOpen)
            ) {
                drawHudCards(it.drawService)
            }
            if (!isDebugEnabled && !isChatOpen && !isPlayerListOpen) {
                drawHudInfo(it.drawService)
            }
        }

        eventBus.register(ClientServerEvent.Join) {
            client.execute { state.resetAll() }
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

            var shouldSendReady = false
            var shouldOpenCard = false

            if (cardHudWasClicked && client.screen is ChatScreen) {
                shouldOpenCard = true
                cardHudWasClicked = false
            }

            // If Y (card hotkey) is pressed, decide whether to open the card screen or ready up
            if (openCardKeyWasPressed && !openCardKeyAlreadyPressed) {
                if (state.ready?.canSendReady == true && optionsAccessor.isSneakPressed()) {
                    shouldSendReady = true
                } else {
                    shouldOpenCard = true
                }

                // Prevent holding down the "Ready" keybind from toggling the ready packet multiple times
                openCardKeyAlreadyPressed = true
            } else if (!openCardKey.isPressed()) {
                openCardKeyAlreadyPressed = false
            }

            if (shouldSendReady && state.ready?.isRunning == true) {
                val packet = SetReadyPacket(!(state.ready?.isReady ?: false))
                when {
                    packetEvents.readySetV2.send(packet) -> {}
                    packetEvents.readySetV1.send(packet) -> {}
                }
            }

            if (shouldOpenCard) {
                openCardScreen(getSelectedCard()?.teamKey)
            }

            if (toggleHudKey.wasPressed()) {
                config.client.enableHud = !config.client.enableHud
                configService.writeConfig(config)
            }

            tooltipHudRenderer.tick()
        }

        eventBus.register(ScreenEvent.AfterInit) {
            if (it.screen.screen is ChatScreen) {
                it.screen.onAfterLeftClick {
                    // If the card is clicked, open the card
                    if (isCardFocused()) {
                        cardHudWasClicked = true
                    }
                }
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