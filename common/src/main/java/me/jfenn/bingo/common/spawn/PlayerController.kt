package me.jfenn.bingo.common.spawn

import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.options.BingoOptions
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.integrations.VanishIntegration
import me.jfenn.bingo.platform.*
import net.minecraft.server.MinecraftServer
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
internal class PlayerController(
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
    private val recipeManager: IRecipeManager,
    private val vanishIntegration: VanishIntegration,
    private val playerManager: IPlayerManager,
    private val serverTaskExecutor: IExecutors.IServerTaskExecutor,
) : BingoComponent() {

    private fun updateVanishStatus(
        player: IPlayerHandle
    ): Boolean {
        val team = teamService.getPlayerTeam(player.player)
        val isOnActiveTeam = team?.isPlaying() == true

        // if the player is not on a team, they should be vanished
        val isVanished = !isOnActiveTeam && (state.state == GameState.LOADING || state.state == GameState.COUNTDOWN || state.state == GameState.PLAYING)
        vanishIntegration.setVanish(player.player, isVanished)
        return isVanished
    }

    fun updateGameMode(
        player: IPlayerHandle,
        forceReset: Boolean = false,
    ) {
        val lobbyWorld = server.lobbyWorld ?: return
        if (!state.isLobbyMode) {
            log.error("[PlayerController] Attempted updateGameMode, but isLobbyMode=false!")
            return
        }

        val team = teamService.getPlayerTeam(player.player)
        val isOnActiveTeam = team?.isPlaying() == true
        log.debug("updateGameMode({}, forceReset={})", player.playerName, forceReset)

        val gameMode = when (state.state) {
            GameState.UNINITIALIZED,
            GameState.PREGAME,
            GameState.STARTING,
            GameState.PRELOADING -> GameMode.ADVENTURE
            GameState.LOADING,
            GameState.COUNTDOWN,
            GameState.PLAYING -> when {
                isOnActiveTeam -> when (state.state) {
                    GameState.PLAYING -> GameMode.SURVIVAL
                    else -> GameMode.ADVENTURE
                }
                !cardViewService.supportsCardHud(player) && vanishIntegration.isInstalled() -> GameMode.ADVENTURE
                else -> GameMode.SPECTATOR
            }
            GameState.POSTGAME -> when {
                cardViewService.supportsCardHud(player) -> GameMode.SPECTATOR
                else -> GameMode.CREATIVE
            }
        }

        // This indicates that a player is starting the game, or has logged back on after a game has started
        // Note: if force-gamemode=true in server.properties, this check might be falsely activated during gameplay
        val preGameStates = arrayOf(GameState.PREGAME, GameState.STARTING, GameState.PRELOADING, GameState.LOADING, GameState.COUNTDOWN)
        val isRespawnNeeded = (player.player.interactionManager.gameMode != gameMode || forceReset) && (state.state in preGameStates || isOnActiveTeam) && state.state != GameState.POSTGAME
        val isStuckInLobby = player.serverWorld == lobbyWorld && state.state != GameState.PREGAME
        val isStuckInWorld = player.serverWorld != lobbyWorld && state.state == GameState.PREGAME
        if (isRespawnNeeded || isStuckInLobby || isStuckInWorld) {
            if (!player.isAlive) {
                log.debug("[PlayerController] respawnPlayer({})", player.playerName)
                val newPlayer = player.respawn()
                serverTaskExecutor.execute { updateGameMode(newPlayer, true) }
                return
            }

            respawnPlayer(player)
        }

        if (player.player.interactionManager.gameMode != gameMode) {
            player.player.changeGameMode(gameMode)
        }

        val isVanished = updateVanishStatus(player)
        // allow players in adventure mode to fly
        if (isVanished && vanishIntegration.isInstalled()) {
            player.player.abilities.allowFlying = true
            player.player.sendAbilitiesUpdate()
        }
    }

    private fun resetPlayerHealth(player: IPlayerHandle) {
        player.getEffects()
            // Night vision needs to be filtered so that this doesn't cancel out [NightVisionController]
            .filterNot { options.isNightVision && it.type == EffectType.NIGHT_VISION }
            // Avoid cancelling out effects from [CountdownController]
            .filterNot { state.state == GameState.COUNTDOWN && it.type == EffectType.INVISIBILITY }
            .forEach {
                log.debug("[PlayerController] Removing effect {} from {}", it.type, player)
                player.removeEffect(it)
            }

        player.fireTicks = 0
        player.isOnFire = false
        player.health = player.maxHealth
        player.air = player.maxAir
        player.foodLevel = player.maxFoodLevel
        player.saturationLevel = player.maxSaturationLevel
        player.exhaustion = 0f
    }

    private fun respawnPlayer(player: IPlayerHandle) {
        log.debug("respawnPlayer({})", player.playerName)

        // reset player info
        resetPlayerHealth(player)
        player.player.inventory.clear()

        // unlock all crafting recipes, if set
        if (state.state == GameState.PLAYING && options.isUnlockRecipes)
            recipeManager.unlockRecipes(player, recipeManager.listRecipes())

        // reset advancements state
        advancementManager.clearAdvancements(player.player)

        if (state.state == GameState.PREGAME || state.state == GameState.STARTING || state.state == GameState.PRELOADING) {
            spawnService.teleportToLobby(player)
        } else {
            // teleport the player to their spawnpoint
            spawnService.teleportPlayer(player)

            // give spawn equipment to the player if they are on a team
            if (teamService.isPlaying(player.player) && state.state == GameState.PLAYING) {
                spawnService.giveSpawnEquipment(listOf(player))
            }
        }
    }

    init {
        events.onPlayerInit { player ->
            // update vanish status on init, so that players are hidden before the login message
            updateVanishStatus(player)
        }

        events.onPlayerJoin { player ->
            val lobbyWorld = server.lobbyWorld ?: return@onPlayerJoin

            // if in pregame, move to the lobby world
            if (state.state == GameState.PREGAME) {
                player.setSpawnPoint(lobbyWorld, state.lobbySpawnPos, state.lobbySpawnYaw, forced = true, sendMessage = false)
            }

            // schedule an updateGameMode (handled in the next onGameTick)
            state.playersJoinedIds.add(player.uuid)
        }

        events.onPlayerRespawn { event ->
            if (!state.isLobbyMode) return@onPlayerRespawn
            val lobbyWorld = server.lobbyWorld ?: return@onPlayerRespawn
            val player = event.player

            // if in pregame, move to the lobby world
            if (state.state == GameState.PREGAME) {
                val spawnPos = state.lobbySpawnPos
                player.setSpawnPoint(lobbyWorld, spawnPos, state.lobbySpawnYaw, forced = true, sendMessage = false)
                player.teleport(lobbyWorld, spawnPos.toVector3d().add(0.5, 0.0, 0.5), state.lobbySpawnYaw, 0f)
            }

            // if playing, give the player new spawn equipment
            if (
                state.state == GameState.PLAYING
                // if KEEP_INVENTORY is on, we don't need to replace these items
                && !server.gameRules.get(GameRules.KEEP_INVENTORY).get()
                && teamService.isPlaying(player.player)
            ) {
                spawnService.giveSpawnEquipment(listOf(player))
            }
        }

        // Transition from PREGAME -> LOADING
        events.onEnter(GameState.LOADING) { prevState ->
            if (prevState == GameState.LOADING) return@onEnter

            for (player in playerManager.getPlayers()) {
                if (state.isLobbyMode) updateGameMode(player, forceReset = true)
            }
        }

        // Transition from PREGAME -> PLAYING
        events.onEnter(GameState.PLAYING) { prevState ->
            if (prevState == GameState.PLAYING) return@onEnter

            for (player in playerManager.getPlayers()) {
                if (state.isLobbyMode) updateGameMode(player, forceReset = true)
            }
        }

        // Transition from PLAYING -> POSTGAME
        events.onEnter(GameState.POSTGAME) {
            for (player in playerManager.getPlayers()) {
                if (state.isLobbyMode) {
                    updateGameMode(player)
                } else {
                    // if running with isLobbyMode=false, take the elytra back after a game
                    elytraService.takeElytra(player)
                }
            }
        }

        events.onGameTick {
            for (player in playerManager.getPlayers()) {
                // If the player has just joined, update their gamemode
                val playerUuid = player.uuid
                if (playerUuid in state.playersJoinedIds) {
                    state.playersJoinedIds.remove(playerUuid)
                    if (state.isLobbyMode) updateGameMode(player)
                }

                // ensure that players don't run out of hunger in the lobby
                if (state.isLobbyMode && state.state == GameState.PREGAME && player.player.hungerManager.foodLevel < 10) {
                    player.player.hungerManager.foodLevel = 10
                }

                if (state.state == GameState.PLAYING && player.player.health > 0f && !player.isSpectator) {
                    // if elytra mode is configured, give the player an elytra
                    if (options.isElytra && teamService.isPlaying(player.player))
                        elytraService.giveElytra(playerManager.forPlayer(player.player))

                    // if the player is spectating in adventure mode,
                    // they should be able to fly
                    if (state.isLobbyMode && player.player.interactionManager.gameMode == GameMode.ADVENTURE) {
                        val isSpectating = !teamService.isPlaying(player.player)
                        if (isSpectating) {
                            resetPlayerHealth(player)
                        }
                        if (isSpectating != player.player.abilities.allowFlying) {
                            player.player.abilities.allowFlying = isSpectating
                            player.player.sendAbilitiesUpdate()
                        }
                    }
                }
            }
        }
    }

}