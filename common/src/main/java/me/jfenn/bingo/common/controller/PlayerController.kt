package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IAdvancementManager
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.card.BingoPlayerProfile
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.map.MapItemService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.common.utils.allInventorySlots
import me.jfenn.bingo.common.utils.giveOrEquipStack
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.integrations.VanishIntegration
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import org.slf4j.Logger
import java.util.*

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
    private val config: BingoConfig,
    private val mapItemService: MapItemService,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
    private val advancementManager: IAdvancementManager,
    private val vanishIntegration: VanishIntegration,
    private val permissions: PermissionsIntegration,
) : BingoComponent() {

    fun givePreviewCardItem(player: ServerPlayerEntity) {
        val hasMap = player.allHeldStacks().any { MapItemService.isMapItem(it) }

        if (!hasMap) {
            val mapItem = mapItemService.createPreviewMapItem()
            player.giveItemStack(mapItem)
        }
    }

    fun removePreviewCardItem(player: ServerPlayerEntity) {
        for ((slot, item) in player.allInventorySlots()) {
            if (MapItemService.isMapItem(item) && FilledMapItem.getMapId(item) == state.getPreviewMap(server).mapId)
                player.inventory.removeStack(slot)
        }
    }

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    fun giveMapCardItem(player: ServerPlayerEntity) {
        val team = teamService.getPlayerTeam(player) ?: return
        val hasMap = player.allHeldStacks().any { mapItemService.isMapTeamItem(it, team) }

        if (!hasMap) {
            // if the player has another team's map item, remove it!
            for ((slot, item) in player.allInventorySlots()) {
                if (MapItemService.isMapItem(item))
                    player.inventory.removeStack(slot)
            }

            // give the player a new map item
            val mapItem = mapItemService.createMapItem(team)
            player.giveItemStack(mapItem)
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    fun giveSpectatorMapItems(player: ServerPlayerEntity) {
        state.getRegisteredTeams()
            .map { mapItemService.createMapItem(it) }
            .forEachIndexed { i, stack ->
                val mapId = FilledMapItem.getMapId(stack)
                val hasMap = player.allHeldStacks().any { FilledMapItem.getMapId(it) == mapId }
                if (!hasMap) player.getStackReference(i).set(stack)
            }
    }

    /**
     * Equip the player with elytra and firework rockets.
     * Only runs if config.isElytra is true
     */
    fun giveElytra(player: ServerPlayerEntity) {
        val hasElytra = player.allHeldStacks().any { it.item == Items.ELYTRA && it.nbt?.getBoolean(NBT_BINGO_VANISH) == true }
        if (!hasElytra) {
            // insert elytra in armor slot
            player.giveOrEquipStack(ItemStack(Items.ELYTRA, 1).also {
                it.getOrCreateNbt().apply {
                    // unbreakable (should not take damage)
                    putBoolean("Unbreakable", true)
                    putBoolean(NBT_BINGO_IGNORE, true)
                    putBoolean(NBT_BINGO_VANISH, true)
                }
            })
        }

        // also give rocket
        val rocketStack = player.allHeldStacks().find { it.item == Items.FIREWORK_ROCKET }

        if (rocketStack != null && rocketStack.count < 2) {
            rocketStack.count = 2
        }

        if (rocketStack == null) {
            player.giveItemStack(ItemStack(Items.FIREWORK_ROCKET, 2).also {
                it.getOrCreateNbt().apply {
                    putBoolean(NBT_BINGO_IGNORE, true)
                    putBoolean(NBT_BINGO_VANISH, true)
                    put("Fireworks", NbtCompound())
                }
            })
        }
    }

    fun updateGameMode(
        player: ServerPlayerEntity,
        forceReset: Boolean = false,
    ) {
        val isOnTeam = teamService.isPlaying(player)
        log.debug("updateGameMode(${player.uuidAsString}, forceReset=$forceReset)")

        val gameMode = when (state.state) {
            GameState.PREGAME -> GameMode.ADVENTURE
            GameState.PLAYING -> when {
                isOnTeam -> GameMode.SURVIVAL
                vanishIntegration.isInstalled() -> GameMode.ADVENTURE
                else -> GameMode.SPECTATOR
            }
            GameState.POSTGAME -> GameMode.CREATIVE
        }

        if (
            player.interactionManager.gameMode != gameMode ||
            (player.serverWorld == lobbyWorld && state.state != GameState.PREGAME) ||
            forceReset
        ) {
            // This indicates that a player is starting the game, or has logged back on after a game has started
            // Note: if force-gamemode=true in server.properties, this check might be falsely activated during gameplay
            if (state.state == GameState.PREGAME || state.state == GameState.PLAYING) {
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

    private fun updateNightVision(player: ServerPlayerEntity) {
        val isOnTeam = teamService.isPlaying(player)

        // give the player night_vision if configured
        if (
            (state.state == GameState.PLAYING && !isOnTeam && config.nightVisionInSpectator) ||
            (state.state == GameState.POSTGAME && config.nightVisionInPostgame) ||
            (state.state == GameState.PLAYING && options.isNightVision)
        ) {
            player.addStatusEffect(
                StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0, false, false),
            )
        }
    }

    fun resetPlayerHealth(player: ServerPlayerEntity) {
        player.clearStatusEffects()
        player.fireTicks = 0
        player.isOnFire = false
        player.health = player.maxHealth
        player.air = player.maxAir
        player.hungerManager.foodLevel = 20
        player.hungerManager.saturationLevel = 5f
        player.hungerManager.exhaustion = 0f
    }

    fun respawnPlayer(player: ServerPlayerEntity) {
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

            // give each player their team's card
            giveMapCardItem(player)

            // give spawn equipment to the player if they are on a team
            if (teamService.isPlaying(player)) {
                spawnService.giveSpawnEquipment(listOf(player))
            }
        }
    }

    private val lobbyWorld get() = server.lobbyWorld!!
    private val playersJoinedIds = mutableSetOf<UUID>()

    init {
        events.onPlayerJoin { player ->
            // if in pregame, move to the lobby world
            if (state.state == GameState.PREGAME) {
                val spawnPos = state.lobbySpawnPos.toBlockPos()
                player.setSpawnPoint(lobbyWorld.registryKey, spawnPos, 180f, true, false)
            }

            // schedule an updateGameMode (handled in the next onGameTick)
            playersJoinedIds.add(player.uuid)
        }

        events.onPlayerRespawn { player ->
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

        // Transition from PREGAME -> PLAYING
        events.onEnter(GameState.PLAYING) { prevState ->
            if (prevState != GameState.PREGAME) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(player, forceReset = true)
            }
        }

        // Transition from PLAYING -> POSTGAME
        events.onEnter(GameState.POSTGAME) { prevState ->
            if (prevState != GameState.PLAYING) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(player)

                // remove any existing maps in the player's inventory
                for (i in 0 until player.inventory.size()) {
                    if (MapItemService.isMapItem(player.inventory.getStack(i)))
                        player.inventory.removeStack(i)
                }

                // give all maps for active teams to the player
                giveSpectatorMapItems(player)
            }
        }

        events.onGameTick {
            for (player in server.playerManager.playerList) {
                val playerTeam = teamService.getPlayerTeam(player)

                // If the player has just joined, update their gamemode
                if (player.uuid in playersJoinedIds) {
                    playersJoinedIds.remove(player.uuid)
                    updateGameMode(player)
                }

                if (state.state == GameState.PREGAME && player.isAlive) {
                    if (options.showPreviewCard) {
                        givePreviewCardItem(player)
                    } else {
                        removePreviewCardItem(player)
                    }
                }

                if (state.state == GameState.PLAYING && player.health > 0f && !player.isSpectator) {
                    // give player a new map, if they don't have one?
                    giveMapCardItem(player)

                    // if elytra mode is configured, give the player an elytra
                    if (options.isElytra && teamService.isPlaying(player))
                        giveElytra(player)

                    // if the player is spectating in adventure mode,
                    // they should be able to fly
                    if (player.interactionManager.gameMode == GameMode.ADVENTURE) {
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

                if (state.state != GameState.PREGAME && (playerTeam == null || state.state == GameState.POSTGAME)) {
                    // if the state is not POSTGAME, players must have permission to receive the spectator cards
                    if (state.state == GameState.POSTGAME || permissions.hasPermission(player, Permission.SPECTATOR_VIEW_CARDS))
                        giveSpectatorMapItems(player)
                }

                // If the player has switched teams, update the player UUID sets...
                // (this is necessary because teams could be changed without us knowing, using /team join)
                val playerProfile = BingoPlayerProfile.fromPlayer(player)
                val isTeamChanged = state.teams.values.any { team ->
                    if (playerTeam?.key == team.key) {
                        team.players.add(playerProfile)
                    } else {
                        team.players.remove(playerProfile)
                    }
                }
                if (isTeamChanged) {
                    teamService.onTeamChanged(player)
                    updateGameMode(player)
                }
            }
        }

        events.onUpdateTick {
            for (player in server.playerManager.playerList) {
                // Night vision is apparently super buggy and doesn't apply if invoked in `resetPlayerHealth()`...
                // (seems like effects get cleared in a few other places - so we need to make sure it persists)
                updateNightVision(player)
            }
        }
    }

}