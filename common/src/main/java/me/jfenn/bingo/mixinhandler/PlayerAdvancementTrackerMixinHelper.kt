package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.ScopeStopped
import net.minecraft.server.network.ServerPlayerEntity

class PlayerAdvancementTrackerMixinHelper(
    private val state: BingoState,
    private val teamService: TeamService,
    eventBus: IEventBus,
) {
    fun shouldPreventSpectatorAdvancements(player: ServerPlayerEntity): Boolean {
        return state.isLobbyMode && !teamService.isPlaying(player)
    }

    init {
        instance = this
        eventBus.register(ScopeStopped) {
            instance = null
        }
    }

    companion object {
        var instance: PlayerAdvancementTrackerMixinHelper? = null

        fun shouldPreventSpectatorAdvancements(player: ServerPlayerEntity): Boolean {
            return instance?.shouldPreventSpectatorAdvancements(player) ?: false
        }
    }
}