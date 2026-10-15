package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.options.LocatorBarMode
import me.jfenn.bingo.common.scoring.ScoreService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.ScopeStopped
import net.minecraft.server.level.ServerPlayer

/**
 * Controls which players are transmitted as waypoints on the vanilla locator bar.
 *
 * Vanilla only offers a global on/off (`locatorBar` gamerule) and per-player transmit/receive
 * ranges, which cannot express "only my team sees me". This helper is consulted by the
 * ServerWaypointManager mixin to block connections between players that should not see
 * each other.
 */
class LocatorBarMixinHelper internal constructor(
    private val state: BingoState,
    private val teamService: TeamService,
    private val playerManager: IPlayerManager,
    eventBus: IEventBus,
) {

    fun shouldCreateConnection(receiver: ServerPlayer, transmitter: ServerPlayer): Boolean {
        val mode = state.options.locatorBarMode
        if (mode == LocatorBarMode.ALL) return true

        // only restrict during an active game; leave the lobby alone
        if (state.state != GameState.PLAYING) return true

        val receiverPlayer = playerManager.forPlayer(receiver)
        val transmitterPlayer = playerManager.forPlayer(transmitter)

        val receiverTeam = teamService.getPlayerTeam(receiverPlayer) ?: return true
        val transmitterTeam = teamService.getPlayerTeam(transmitterPlayer) ?: return true

        // always allow teammates to see each other
        if (receiverTeam.key == transmitterTeam.key) return true

        // show the currently-leading team to everyone
        if (mode == LocatorBarMode.TEAM_AND_WINNER) {
            val leadingTeam = ScoreService.getLeading(state, allowTies = true)
            if (leadingTeam?.key == transmitterTeam.key) return true
        }

        return false
    }

    init {
        instance = this
        eventBus.register(ScopeStopped) {
            instance = null
        }
    }

    companion object {
        var instance: LocatorBarMixinHelper? = null

        fun shouldCreateConnection(receiver: ServerPlayer, transmitter: ServerPlayer): Boolean =
            instance?.shouldCreateConnection(receiver, transmitter) ?: true
    }
}
