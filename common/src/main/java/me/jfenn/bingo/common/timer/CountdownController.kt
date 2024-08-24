package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.*
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.utils.plus
import net.minecraft.server.MinecraftServer
import org.joml.Vector3d
import kotlin.time.Duration.Companion.seconds

class CountdownController(
    private val server: MinecraftServer,
    private val options: BingoOptions,
    private val state: BingoState,
    private val events: ScopedEvents,
    private val countdownService: CountdownService,
    private val teamService: TeamService,
    private val tickManager: ITickManager,
    private val statusEffectsFactory: IStatusEffectsFactory,
    private val spawnService: SpawnService,
    private val entityManager: IEntityManager,
) : BingoComponent() {

    private var countdownUntil: Instant? = null
    private var prevSeconds: Int = -1

    private val entityMap = mutableMapOf<BingoTeamKey, IBatEntity>()

    private val playingPlayerList get() = server.playerManager.playerList
        .filter { teamService.isPlaying(it) }

    private fun forcePlayersInvisible() {
        for (player in playingPlayerList) {
            // Make the player invisible during the countdown
            // (otherwise, the player blocks other players' visibility)
            val statusEffects = statusEffectsFactory.getStatusEffects(player)
            statusEffects.addEffect(
                type = EffectType.INVISIBILITY,
                duration = -1,
                amplifier = 0,
                ambient = false,
                visible = false,
            )
        }
    }

    private fun forcePlayersOnBats() {
        state.getRegisteredTeams().forEach { team ->
            val entity = entityMap.getOrPut(team.key) {
                // Create a new bat entity in the spawn dimension
                val world = spawnService.getSpawnDimension()
                val marker = entityManager.createEntity(EntityType.BAT, world)
                marker.apply {
                    invulnerable = true
                    noGravity = true
                    silent = true
                    noAI = true
                    persistenceRequired = true
                }

                val spawnPoint = team.spawnpoint ?: return@forEach
                marker.pos = spawnPoint.toVector3d() + Vector3d(0.5, -0.4, 0.5)
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

            for (player in team.players.mapNotNull { server.playerManager.getPlayer(it.uuid) }) {
                player.startRiding(entity.entity, true)
            }
        }
    }

    init {
        events.onEnter(GameState.COUNTDOWN) {
            countdownUntil = Clock.System.now() + options.startCountdownSeconds.seconds
            prevSeconds = -1

            if (state.isLobbyMode)
                forcePlayersInvisible()
        }

        events.onStateChange { (_, toState) ->
            val isLockedState = toState == GameState.LOADING || toState == GameState.COUNTDOWN

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

        events.onGameTick {
            if (state.isLobbyMode) {
                if (state.state == GameState.LOADING || state.state == GameState.COUNTDOWN) {
                    forcePlayersOnBats()
                }
            }

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