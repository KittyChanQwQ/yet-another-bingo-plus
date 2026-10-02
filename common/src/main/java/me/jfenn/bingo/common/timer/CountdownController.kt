package me.jfenn.bingo.common.timer

import me.jfenn.bingo.api.state.GameAction
import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.*
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal class CountdownController(
    events: ScopedEvents,
    private val config: BingoConfig,
    private val state: BingoState,
    private val eventBus: IEventBus,
    private val countdownService: CountdownService,
    private val teamService: TeamService,
    private val tickManager: ITickManager,
    private val spawnService: SpawnService,
    private val entityManager: IEntityManager,
    private val playerManager: IPlayerManager,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
) : BingoComponent() {

    private var countdownDelayTicks: Int? = null

    private val entityMap = mutableMapOf<UUID, IBatEntity>()

    private val playingPlayerList get() = playerManager.getPlayers()
        .filter { teamService.isPlaying(it) }

    private fun forcePlayersInvisible() {
        for (player in playingPlayerList) {
            // Make the player invisible during the countdown
            // (otherwise, the player blocks other players' visibility)
            player.addEffect(
                type = EffectType.INVISIBILITY,
                duration = -1,
                amplifier = 0,
                ambient = false,
                visible = false,
            )
        }
    }

    private fun forcePlayersOnBats() {
        val world = spawnService.getSpawnDimension()

        state.getRegisteredTeams().forEach { team ->
            for (teamPlayer in team.players) {
                val player = playerManager.getPlayer(teamPlayer.uuid) ?: continue

                val entity = entityMap.getOrPut(teamPlayer.uuid) {
                    // Create a new bat entity in the spawn dimension
                    val marker = entityManager.createEntity(EntityType.BAT, world)
                    marker.apply {
                        invulnerable = true
                        noGravity = true
                        silent = true
                        noAI = true
                        persistenceRequired = true
                    }

                    val spawnPoint = team.spawnpoint ?: return@forEach
                    marker.isResting = false
                    marker.pos = spawnPoint.toVector3d() + teamPlayer.randomOffset + Vector3d(0.0, -0.3, 0.0)
                    entityManager.spawnEntity(world, marker)

                    // Make the bat invisible
                    marker.addEffect(
                        type = EffectType.INVISIBILITY,
                        duration = -1,
                        amplifier = 0,
                        ambient = false,
                        visible = false,
                    )

                    marker
                }

                player.startRiding(entity, true)
            }
        }
    }

    private fun scheduleCountdown() {
        for (seconds in 0..config.countdownSeconds) {
            val remainingSeconds = config.countdownSeconds - seconds

            CompletableFuture.delayedExecutor(seconds.toLong(), TimeUnit.SECONDS, serverTaskExecutor)
                .execute {
                    if (state.state != GameState.COUNTDOWN)
                        return@execute

                    countdownService.sendCountdownPacket(CountdownPacket(remainingSeconds))

                    // When the countdown ends, start the game!
                    if (remainingSeconds == 0) {
                        state.changeState(eventBus, GameAction.START)
                    }
                }
        }
    }

    fun shouldPreventActions() = state.state == GameState.LOADING || state.state == GameState.COUNTDOWN

    init {
        events.onEnter(GameState.COUNTDOWN) {
            if (config.countdownDelayTicks > 0 || config.countdownSeconds > 0) {
                countdownDelayTicks = config.countdownDelayTicks

                if (state.isLobbyMode)
                    forcePlayersInvisible()
            } else {
                // if no countdown is configured, start the game immediately
                countdownService.sendCountdownPacket(CountdownPacket(0))
                state.changeState(eventBus, GameAction.START)
            }
        }

        events.onStateChange { (_, toState) ->
            val isLockedState = toState == GameState.LOADING || toState == GameState.COUNTDOWN || toState == GameState.POSTGAME

            if (state.isLobbyMode)
                tickManager.setFrozen(isLockedState)

            if (!isLockedState) {
                // Remove all spawn marker entities when the game starts
                for ((_, entity) in entityMap) {
                    entity.discard()
                }
                entityMap.clear()
            }
        }

        eventBus.register(TickEvent.Start) {
            if (state.isLobbyMode && shouldPreventActions()) {
                forcePlayersOnBats()
            }

            countdownDelayTicks?.minus(1)?.let { delay ->
                if (delay < 0) {
                    scheduleCountdown()
                    countdownDelayTicks = null
                } else {
                    countdownDelayTicks = delay
                }
            }
        }

        if (state.isLobbyMode) {
            eventBus.register(UseBlockEvent) {
                if (shouldPreventActions())
                    ActionResult.Fail(Unit)
                else null
            }

            eventBus.register(UseEntityEvent) {
                if (shouldPreventActions())
                    ActionResult.Fail(Unit)
                else null
            }

            eventBus.register(UseItemEvent) {
                if (shouldPreventActions())
                    ActionResult.Fail(null)
                else null
            }

            eventBus.register(AttackEntityEvent) {
                if (shouldPreventActions())
                    ActionResult.Fail(Unit)
                else null
            }
        }
    }

}