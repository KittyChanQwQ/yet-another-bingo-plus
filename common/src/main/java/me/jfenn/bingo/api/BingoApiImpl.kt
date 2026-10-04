package me.jfenn.bingo.api

import me.jfenn.bingo.api.card.ICardService
import me.jfenn.bingo.api.config.IBingoConfig
import me.jfenn.bingo.api.data.IBingoGame
import me.jfenn.bingo.api.data.IBingoPlayerStats
import me.jfenn.bingo.api.data.IBingoTeam
import me.jfenn.bingo.api.data.IBingoTeams
import me.jfenn.bingo.api.event.GameEndedEvent
import me.jfenn.bingo.api.event.GameStartedEvent
import me.jfenn.bingo.api.event.TeamChangedEvent
import me.jfenn.bingo.api.options.IOptionsService
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.api.state.GameStateChangedEvent
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.stats.StatsService
import net.minecraft.server.MinecraftServer
import java.util.*

internal class BingoApiImpl(
    override val server: MinecraftServer,
    private val state: BingoState,
    private val statsService: StatsService,
    override val game: IBingoGame,
    override val cards: ICardService,
    override val options: IOptionsService,
    events: ScopedEvents,
) : IBingoApi, IBingoTeams, IBingoConfig {

    override val teams: IBingoTeams get() = this

    override fun getPlayerStats(id: UUID): IBingoPlayerStats {
        val stats = statsService.getPlayerSummary(id)
        return BingoPlayerStatsImpl(stats)
    }

    override val config: IBingoConfig get() = this

    override fun iterator(): Iterator<IBingoTeam> {
        return state.getRegisteredTeams()
            .map { BingoTeamImpl(it) }
            .iterator()
    }

    override val isLobbyMode: Boolean
        get() = state.isLobbyMode

    init {
        events.onEnter(GameState.PREGAME) {
            BingoEvents.GAME_RESET.invoke(null)
        }

        events.onEnter(GameState.STARTING, true) {
            BingoEvents.GAME_STARTING.invoke(null)
        }

        events.onEnter(GameState.PLAYING, true) {
            BingoEvents.GAME_STARTED.invoke(GameStartedEvent(game.id))
        }

        events.onEnter(GameState.POSTGAME, true) {
            BingoEvents.GAME_ENDED.invoke(GameEndedEvent(game.id))
        }

        events.onStateChange {
            val event = GameStateChangedEvent(it.from, it.to)
            BingoEvents.STATE_CHANGED.invoke(event)
            BingoEvents.onStateChanged(it.to).invoke(event)
        }

        events.onChangeTeam { event ->
            BingoEvents.TEAM_CHANGED.invoke(TeamChangedEvent(event.player.uuid))
        }
    }
}