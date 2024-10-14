package me.jfenn.bingo.common.ready

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.IPlayerHandle
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ReadyTimerState {

    private var totalPlayers = listOf<UUID>()
    private val readyPlayers = mutableSetOf<UUID>()
    private var startedAt: Instant? = null
    private var totalDuration: Duration? = null
    private var additionalDuration: Duration = Duration.ZERO

    val isRunning: Boolean get() = startedAt != null

    fun updatePlayers(players: List<IPlayerHandle>) {
        totalPlayers = players.map { it.uuid }
    }

    fun isReady(player: UUID): Boolean = player in readyPlayers

    fun setReady(player: UUID, ready: Boolean) {
        if (ready) readyPlayers.add(player) else readyPlayers.remove(player)

        val remaining = remainingTime() ?: return
        if (remaining < 10.seconds) {
            additionalDuration += (10.seconds - remaining)
        }
    }

    fun startTimer(duration: Duration) {
        startedAt = Clock.System.now()
        totalDuration = duration
    }

    fun duration(): Duration? {
        val readyRatio = totalPlayers.count { isReady(it) } / totalPlayers.size.toFloat().coerceAtLeast(1f)
        val millis = (1f - readyRatio) * (totalDuration?.inWholeMilliseconds ?: return null)
        return millis.toLong().milliseconds + additionalDuration
    }

    fun totalTime(): Duration? = totalDuration

    fun elapsedTime(): Duration? {
        val now = Clock.System.now()
        return now - (startedAt ?: return null)
    }

    fun remainingTime(): Duration? {
        val totalDuration = duration() ?: return null
        val elapsedDuration = elapsedTime() ?: return null
        return totalDuration - elapsedDuration
    }

    fun reset() {
        readyPlayers.clear()
        startedAt = null
        totalDuration = null
        additionalDuration = Duration.ZERO
    }

}