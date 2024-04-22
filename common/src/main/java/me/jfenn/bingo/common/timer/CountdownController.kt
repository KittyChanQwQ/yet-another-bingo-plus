package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.EffectType
import me.jfenn.bingo.api.IStatusEffectsFactory
import me.jfenn.bingo.api.ITickManager
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.server.MinecraftServer
import kotlin.time.Duration.Companion.seconds

class CountdownController(
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val events: ScopedEvents,
    private val countdownService: CountdownService,
    private val tickManager: ITickManager,
    private val statusEffectsFactory: IStatusEffectsFactory,
) : BingoComponent() {

    private var countdownUntil: Instant? = null
    private var prevSeconds: Int = -1

    init {
        events.onEnter(GameState.COUNTDOWN) {
            countdownUntil = Clock.System.now() + options.startCountdownSeconds.seconds
            prevSeconds = -1

            for (player in server.playerManager.playerList) {
                val statusEffects = statusEffectsFactory.getStatusEffects(player)
                statusEffects.addEffect(
                    type = EffectType.SLOWNESS,
                    duration = -1,
                    amplifier = 10,
                    ambient = false,
                    visible = false,
                )
                statusEffects.addEffect(
                    type = EffectType.JUMP_BOOST,
                    duration = -1,
                    amplifier = 120,
                    ambient = false,
                    visible = false,
                )
            }
        }

        events.onStateChange { (_, toState) ->
            tickManager.setFrozen(toState == GameState.COUNTDOWN)
        }

        events.onGameTick {
            if (state.state != GameState.COUNTDOWN) return@onGameTick
            if (server.ticks % 20 != 0) return@onGameTick

            val until = countdownUntil ?: return@onGameTick
            val now = Clock.System.now()

            val seconds = (until - now).inWholeSeconds.toInt()

            if (prevSeconds != seconds) {
                prevSeconds = seconds

                val packet = CountdownPacket(seconds.coerceAtLeast(0))
                for (player in server.playerManager.playerList) {
                    countdownService.sendCountdownPacket(player, packet)
                }

                // When the countdown ends, start the game!
                if (seconds <= 0) {
                    state.changeState(events, GameState.PLAYING)
                    countdownUntil = null
                }
            }
        }
    }

}