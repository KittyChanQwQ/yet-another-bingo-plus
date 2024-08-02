package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.api.*
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.integrations.VanishIntegration
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import org.slf4j.Logger

/**
 * Controls player gamemode, status, and inventory setup.
 *
 * Ensures that players are put in their correct gamemode on certain events:
 * - in pre-game -> ADVENTURE
 * - when the game starts -> SURVIVAL & clear inventory
 * - if spectating -> SPECTATOR (or ADVENTURE + flying, if using the vanish integration)
 * - in post-game -> CREATIVE
 *
 * Needs to additionally ensure that the state is re-applied on
 * login, as players could miss game state transitions by logging out.
 */
class PlayerController(
    events: ScopedEvents,
    private val log: Logger,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val options: BingoOptions,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
    private val cardViewService: CardViewService,
    private val elytraService: ElytraService,
    private val advancementManager: IAdvancementManager,
    private val vanishIntegration: VanishIntegration,
    private val statusEffectsFactory: IStatusEffectsFactory,
    private val playerManager: IPlayerManager,
) : BingoComponent() {

    fun updateGameMode(
        player: ServerPlayerEntity,
        forceReset: Boolean = false,
    ) {
        val lobbyWorld = server.lobbyWorld ?: return
        if (!state.isLobbyMode) {
            log.error("[PlayerController] Attempted updateGameMode, but isLobbyMode=false!")
            return
        }

        val isOnTeam = teamService.isPlaying(player)
        log.debug("updateGameMode(${player.uuidAsString}, forceReset=$forceReset)")

        val gameMode = when (state.state) {
            GameState.PREGAME,
            GameState.LOADING,
            GameState.COUNTDOWN -> GameMode.ADVENTURE
            GameState.PLAYING -> when {
                isOnTeam -> GameMode.SURVIVAL
                !cardViewService.supportsCardHud(player) && vanishIntegration.isInstalled() -> GameMode.ADVENTURE
                else -> GameMode.SPECTATOR
            }
            GameState.POSTGAME -> when {
                cardViewService.supportsCardHud(player) -> GameMode.SPECTATOR
                else -> GameMode.CREATIVE
            }
        }

        if (
            player.interactionManager.gameMode != gameMode ||
            (player.serverWorld == lobbyWorld && state.state != GameState.PREGAME) ||
            forceReset
        ) {
            // This indicates that a player is starting the game, or has logged back on after a game has started
            // Note: if force-gamemode=true in server.properties, this check might be falsely activated during gameplay
            if (state.state != GameState.POSTGAME) {
                respawnPlayer(player)
            }

            player.changeGameMode(gameMode)
        }

        // if the player is not on a team, they should be vanished
        val isVanished = !isOnTeam && state.state == GameState.PLAYING
        vanishIntegration.setVanish(player, isVanished)

        // allow players in adventure mode to fly
        if (isVanished && vanishIntegration.isInstalled()) {
            player.abilities.allowFlying = true
            player.sendAbilitiesUpdate()
        }
    }

    private fun resetPlayerHealth(player: ServerPlayerEntity) {
        val statusEffects = statusEffectsFactory.getStatusEffects(player)
        statusEffects.getEffects()
            // Night vision needs to be filtered so that this doesn't cancel out [NightVisionController]
            .filterNot { options.isNightVision && it.type == EffectType.NIGHT_VISION }
            // Avoid cancelling out effects from [CountdownController]
            .filterNot { state.state == GameState.COUNTDOWN && it.type == EffectType.INVISIBILITY }
            .forEach { statusEffects.removeEffect(it) }

        player.fireTicks = 0
        player.isOnFire = false
        player.health = player.maxHealth
        player.air = player.maxAir
        player.hungerManager.foodLevel = 20
        player.hungerManager.saturationLevel = 5f
        player.hungerManager.exhaustion = 0f
    }

    private fun respawnPlayer(player: ServerPlayerEntity) {
        log.debug("respawnPlayer(${player.uuidAsString})")
        // reset player info
        resetPlayerHealth(player)
        player.inventory.clear()

        // unlock all crafting recipes, if set
        if (state.state == GameState.PLAYING && options.isUnlockRecipes)
            player.unlockRecipes(server.recipeManager.values())

        // reset advancements state
        advancementManager.clearAdvancements(player)

        if (state.state == GameState.PREGAME) {
            spawnService.teleportToLobby(player)
        } else {
            // teleport the player to their spawnpoint
            spawnService.teleportPlayer(player)

            // give spawn equipment to the player if they are on a team
            if (teamService.isPlaying(player) && state.state == GameState.PLAYING) {
                spawnService.giveSpawnEquipment(listOf(player))
            }
        }
    }

    init {
        events.onPlayerJoin { player ->
            val lobbyWorld = server.lobbyWorld ?: return@onPlayerJoin

            // if in pregame, move to the lobby world
            if (state.state == GameState.PREGAME) {
                val spawnPos = state.lobbySpawnPos.toBlockPos()
                player.setSpawnPoint(lobbyWorld.registryKey, spawnPos, 180f, true, false)
            }

            // schedule an updateGameMode (handled in the next onGameTick)
            state.playersJoinedIds.add(player.uuid)
        }

        events.onPlayerRespawn { player ->
            val lobbyWorld = server.lobbyWorld ?: return@onPlayerRespawn

            // if in pregame, move to the lobby world
            if (state.state == GameState.PREGAME) {
                val spawnPos = state.lobbySpawnPos.toBlockPos()
                player.setSpawnPoint(lobbyWorld.registryKey, spawnPos, 180f, true, false)
                player.teleport(lobbyWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, 180f, 0f)
            }

            // if playing, give the player new spawn equipment
            if (
                state.state == GameState.PLAYING
                // if KEEP_INVENTORY is on, we don't need to replace these items
                && !server.gameRules.get(GameRules.KEEP_INVENTORY).get()
                && teamService.isPlaying(player)
            ) {
                spawnService.giveSpawnEquipment(listOf(player))
            }
        }

        // Transition from PREGAME -> LOADING
        events.onEnter(GameState.LOADING) { prevState ->
            if (prevState == GameState.LOADING) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(player, forceReset = true)
            }
        }

        // Transition from PREGAME -> PLAYING
        events.onEnter(GameState.PLAYING) { prevState ->
            if (prevState == GameState.PLAYING) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(player, forceReset = true)
            }
        }

        // Transition from PLAYING -> POSTGAME
        events.onEnter(GameState.POSTGAME) {
            for (player in server.playerManager.playerList) {
                updateGameMode(player)

                if (!state.isLobbyMode) {
                    // if running with isLobbyMode=false, take the elytra back after a game
                    elytraService.takeElytra(playerManager.forPlayer(player))
                }
            }
        }

        events.onGameTick {
            for (player in server.playerManager.playerList) {
                // If the player has just joined, update their gamemode
                val playerUuid = player.uuid
                if (playerUuid in state.playersJoinedIds) {
                    state.playersJoinedIds.remove(playerUuid)
                    updateGameMode(player)
                }

                // ensure that players don't run out of hunger in the lobby
                if (state.isLobbyMode && state.state == GameState.PREGAME && player.hungerManager.foodLevel < 10) {
                    player.hungerManager.foodLevel = 10
                }

                if (state.state == GameState.PLAYING && player.health > 0f && !player.isSpectator) {
                    // if elytra mode is configured, give the player an elytra
                    if (options.isElytra && teamService.isPlaying(player))
                        elytraService.giveElytra(playerManager.forPlayer(player))

                    // if the player is spectating in adventure mode,
                    // they should be able to fly
                    if (state.isLobbyMode && player.interactionManager.gameMode == GameMode.ADVENTURE) {
                        val isSpectating = !teamService.isPlaying(player)
                        if (isSpectating) {
                            resetPlayerHealth(player)
                        }
                        if (isSpectating != player.abilities.allowFlying) {
                            player.abilities.allowFlying = isSpectating
                            player.sendAbilitiesUpdate()
                        }
                    }
                }
            }
        }
    }

}