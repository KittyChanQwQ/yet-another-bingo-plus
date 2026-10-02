package me.jfenn.bingo.client.common.hud.screen

import me.jfenn.bingo.client.common.event.HudStateChangedEvent
import me.jfenn.bingo.client.common.packet.ClientPacketEvents
import me.jfenn.bingo.client.common.state.BingoHudState
import me.jfenn.bingo.client.common.utils.Interpolate
import me.jfenn.bingo.client.platform.IClient
import me.jfenn.bingo.client.platform.renderer.IDrawService
import me.jfenn.bingo.client.platform.screen.*
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.ready.SetReadyPacket
import me.jfenn.bingo.common.ready.VoteOption
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.milliseconds
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.Identifier
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.text.Formatting
import org.joml.Vector2i
import org.koin.core.Koin
import java.util.*
import kotlin.math.roundToInt

internal class BingoVoteOptionsScreen(
    koin: Koin,
    private val gameOver: BingoHudState.GameOver?,
    private val canEscape: Boolean = false,
    private val text: TextProvider,
    private val packets: ClientPacketEvents,
    private val state: BingoHudState,
    private val config: BingoConfig,
    private val client: IClient,
    private val helper: IMutableScreenHelper,
    private val buttonFactory: IButtonFactory,
    eventBus: IEventBus,
) : IScreen {

    class Factory(
        private val koin: Koin,
        private val text: TextProvider,
        private val packets: ClientPacketEvents,
        private val state: BingoHudState,
        private val config: BingoConfig,
        private val client: IClient,
        private val screenFactory: IScreenFactory,
        private val buttonFactory: IButtonFactory,
        private val eventBus: IEventBus,
    ) {
        fun create(
            gameOver: BingoHudState.GameOver?,
            canEscape: Boolean,
        ) = screenFactory.build(text.string(StringKey.CardTitle)) { helper ->
            BingoVoteOptionsScreen(
                koin = koin,
                gameOver = gameOver,
                canEscape = canEscape,
                text = text,
                packets = packets,
                state = state,
                config = config,
                client = client,
                helper = helper,
                buttonFactory = buttonFactory,
                eventBus = eventBus,
            )
        }
    }

    private val width by helper::width
    private val height by helper::height

    private val sidebarOffsetInterpolate = Interpolate(
        from = 0f,
        to = 1f,
        duration = 150.milliseconds,
    )
    private var sidebarWidth = 0
    private var sidebarX = 0

    private val closeButton = buttonFactory.createNinePatchButton(
        sliceSize = 8,
        textureSize = Vector2i(200, 20),
        texture = "minecraft:bingo/button_game_generic",
        focusedTexture = "minecraft:bingo/button_game_generic_focused"
    ).also {
        it.message = text.string(StringKey.CardClose)
        it.onClick {
            helper.close()
        }
    }

    private val hudStateListener = eventBus.register(HudStateChangedEvent) {
        init()
    }

    private fun sendReady(voteOption: VoteOption?) {
        val packet = SetReadyPacket(true, voteOption?.id)
        when {
            packets.readySetV2.send(packet) -> {}
            packets.readySetV1.send(packet) -> {}
        }
        // If the screen is paused, it must be closed so that the server can process the ready packet
        if (client.isInSingleplayer && shouldPause()) {
            helper.close()
        }
    }

    private val sidebarDrawable = object : IDrawable {
        override fun render(drawService: IDrawService) {
            // Ensure that the texture is rendered with an even width
            val evenOffset = if (sidebarWidth % 2 == 0) 0 else 1
            drawService.drawNinePatch(
                texture = Identifier.of("minecraft", "bingo/game_sidebar_background"),
                x = sidebarX - evenOffset,
                y = 0,
                width = sidebarWidth + evenOffset,
                height = height,
                sliceSize = 32,
                textureWidth = 128,
                textureHeight = 128,
            )
        }
    }

    private val statusWidget = GameInfoWidget(koin)

    private val optionButtons = mutableMapOf<UUID, IButton>()

    private fun updatePositions() {
        sidebarWidth = (width * 0.2f).toInt() + 100
        sidebarX = (width - sidebarWidth) -
            sidebarOffsetInterpolate.get(Interpolate.Easing.IN_OUT)
                .times((width/2) - (sidebarWidth/2))
                .roundToInt()

        val marginX = 12
        val marginY = 4

        val buttonWidth = sidebarWidth - marginX * 2

        statusWidget.size.x = buttonWidth
        statusWidget.position = Vector2i(sidebarX + marginX, 12)

        closeButton.size = Vector2i(buttonWidth, 20)
        var buttonY = height - closeButton.size.y - 12
        closeButton.position = Vector2i(sidebarX + marginX, buttonY)

        val totalOptionsHeight = state.ready?.voteOptions.orEmpty().size * (40 + marginY)
        val voteOptionsY = (height / 2) - (totalOptionsHeight / 2)
        buttonY = voteOptionsY

        for (voteOption in state.ready?.voteOptions.orEmpty()) {
            val optionButton = optionButtons.getOrPut(voteOption.id) {
                buttonFactory.createNinePatchButton(
                    sliceSize = 8,
                    textureSize = Vector2i(200, 20),
                    texture = "minecraft:bingo/button_game_generic",
                    focusedTexture = "minecraft:bingo/button_game_generic_focused"
                ).also {
                    it.onClick { sendReady(voteOption) }
                }
            }

            optionButton.message = voteOption.title
            optionButton.size = Vector2i(buttonWidth, 40)
            optionButton.position = Vector2i(sidebarX + marginX, buttonY)
            buttonY += optionButton.size.y + marginY
        }
    }

    override fun init() {
        helper.clearChildren()
        updatePositions()

        helper.addDrawable(sidebarDrawable)
        helper.addButton(closeButton)

        optionButtons.values.forEach { helper.addButton(it) }
    }

    override fun resize(width: Int, height: Int) {
        init()
    }

    override fun render(drawService: IDrawService, mouseX: Int, mouseY: Int, delta: Float) {
        // If the gameOver state is cleared, close the screen!
        if (gameOver != null && state.gameOver == null) {
            helper.close()
        }
        // If there are no options available, close the screen!
        val voteOptions = state.ready?.voteOptions.orEmpty()
        if (voteOptions.isEmpty()) {
            helper.close()
        }

        if (sidebarOffsetInterpolate.update()) {
            updatePositions()
        }

        val maxVotes = voteOptions.maxOf { it.votes }
        val voteToCharRatio = maxVotes.coerceAtMost(8).toFloat() / maxVotes.coerceAtLeast(1)

        for (voteOption in voteOptions) {
            val button = optionButtons[voteOption.id] ?: continue
            button.active = !voteOption.isSelected

            val votesChars = (voteOption.votes * voteToCharRatio).roundToInt()
            if (votesChars > 0) {
                val votesText = text.literal(List(votesChars) { "·" }.joinToString(" "))
                val votesWidth = drawService.font.getTextWidth(votesText)
                val votesX = button.position.x + (button.size.x / 2) - (votesWidth / 2)
                val votesY = button.position.y + button.size.y - 10
                drawService.drawText(votesText, x = votesX, y = votesY, -1)
            }
        }

        state.ready?.takeIf { it.isRunning }?.subtitle?.let { title ->
            statusWidget.text = listOf(title.formatted(Formatting.GRAY))
            statusWidget.render(drawService)
        }
    }

    override fun shouldPause() = config.client.cardPausesGame

    override fun shouldCloseOnEsc() = canEscape

    override fun onClose() {
        hudStateListener.close()
    }

}