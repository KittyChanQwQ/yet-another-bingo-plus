package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.MinecraftServer
import kotlin.time.Duration.Companion.seconds

class CountdownController(
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val events: ScopedEvents,
    private val countdownService: CountdownService,
) : BingoComponent() {

    private var countdownUntil: Instant? = null
    private var prevSeconds: Int = -1

    init {
        events.onEnter(GameState.COUNTDOWN) {
            countdownUntil = Clock.System.now() + options.startCountdownSeconds.seconds
            prevSeconds = -1

            for (player in server.playerManager.playerList) {
                player.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        -1,
                        10, false, false
                    )
                )

                player.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.JUMP_BOOST,
                        -1,
                        128, false, false
                    )
                )
            }
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