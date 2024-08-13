package me.jfenn.bingo.common.timer

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.EffectType
import me.jfenn.bingo.api.IStatusEffectsFactory
import me.jfenn.bingo.api.ITickManager
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
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
) : BingoComponent() {

    private var countdownUntil: Instant? = null
    private var prevSeconds: Int = -1

    private val entityMap = mutableMapOf<BingoTeamKey, Entity>()

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
                val marker = EntityType.BAT.create(world) ?: return@forEach
                marker.readNbt(NbtCompound().apply {
                    putBoolean("Invulnerable", true)
                    putBoolean("NoGravity", true)
                    putBoolean("Silent", true)
                    putBoolean("NoAI", true)
                    putBoolean("PersistenceRequired", true)
                })
                val spawnPoint = team.spawnpoint ?: return@forEach
                with(spawnPoint) { marker.setPosition(x.toDouble() + 0.5, y.toDouble() - 0.4, z.toDouble() + 0.5) }
                world.spawnEntity(marker)

                // Make the bat invisible
                statusEffectsFactory.getStatusEffects(marker).apply {
                    addEffect(
                        type = EffectType.INVISIBILITY,
                        duration = -1,
                        amplifier = 0,
                        ambient = false,
                        visible = false,
                    )
                }

                marker
            }

            for (player in team.players.mapNotNull { server.playerManager.getPlayer(it.uuid) }) {
                player.startRiding(entity, true)
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