package me.jfenn.bingo.integrations

import me.jfenn.bingo.Bingo
import me.jfenn.bingo.NBT_BINGO_IGNORE
import me.jfenn.bingo.NBT_BINGO_VANISH
import me.jfenn.bingo.Sounds
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.spawn.SpawnService
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.allInventorySlots
import me.jfenn.bingo.utils.giveOrEquipStack
import me.jfenn.bingo.utils.runSilentCommand
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules

/**
 * Controls player gamemode, status, and inventory setup.
 *
 * Ensures that players are put in their correct gamemode on certain events:
 * - in pre-game -> ADVENTURE
 * - when the game starts -> SURVIVAL & clear inventory
 * - if spectating -> SPECTATOR
 * - in post-game -> CREATIVE
 *
 * Needs to additionally ensure that the state is re-applied on
 * login, as players could miss game state transitions by logging out.
 */
object PlayerController {

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    fun giveMapCardItem(ctx: BingoContext, player: ServerPlayerEntity) {
        val team = BingoTeam.fromPlayer(player) ?: return
        val hasMap = player.allHeldStacks().any { MapItemService.isMapTeamItem(ctx, it, team) }

        if (!hasMap) {
            // if the player has another team's map item, remove it!
            for ((slot, item) in player.allInventorySlots()) {
                if (MapItemService.isMapItem(item))
                    player.inventory.removeStack(slot)
            }

            // give the player a new map item
            val mapItem = MapItemService.createMapItem(ctx, team)
            player.giveItemStack(mapItem)
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    fun giveSpectatorMapItems(ctx: BingoContext, player: ServerPlayerEntity) {
        BingoTeam.entries
            .filter { it.team(player.server).playerList.isNotEmpty() }
            .map { MapItemService.createMapItem(ctx, it) }
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

    fun updateGameMode(ctx: BingoContext, player: ServerPlayerEntity) {
        val isOnTeam = BingoTeam.entries.any { it.includesPlayer(player) }

        val gameMode = when (ctx.state.state) {
            GameState.PREGAME -> GameMode.ADVENTURE
            GameState.PLAYING -> if (isOnTeam) GameMode.SURVIVAL else GameMode.SPECTATOR
            GameState.POSTGAME -> GameMode.CREATIVE
        }

        if (player.interactionManager.gameMode != gameMode) {
            // This indicates that a player is starting the game, or has logged back on after a game has started
            // Note: if force-gamemode=true in server.properties, this check might be falsely activated during gameplay
            if (gameMode == GameMode.SURVIVAL) {
                resetPlayer(ctx, player)
            }

            player.changeGameMode(gameMode)
        }
    }

    fun resetPlayer(ctx: BingoContext, player: ServerPlayerEntity) {
        // reset player info
        player.clearStatusEffects()
        player.inventory.clear()
        player.fireTicks = 0
        player.isOnFire = false
        player.health = 20f
        player.hungerManager.foodLevel = 20
        player.hungerManager.saturationLevel = 5f
        player.hungerManager.exhaustion = 0f

        // unlock all crafting recipes, if set
        if (ctx.config.isUnlockRecipes)
            player.unlockRecipes(ctx.server.recipeManager.values())

        // reset advancements state
        // The yarn mappings for these APIs conflict between versions, so we're using the command for this instead
        // for (advancement in minecraftServer.advancementLoader.advancements) {
        //     val progress = player.advancementTracker
        //     for (criteria in progress.obtainedCriteria) {
        //         player.advancementTracker.revokeCriterion(advancement, criteria)
        //     }
        // }
        ctx.server.runSilentCommand("advancement revoke ${player.nameForScoreboard} everything")

        // teleport the player to their spawnpoint
        SpawnService.teleportPlayer(ctx, player)

        // give each player their team's card
        giveMapCardItem(ctx, player)

        val team = BingoTeam.fromPlayer(player)
        player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
        player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game has started!")))
        Sounds.playGameStarted(player)

        // give spawn equipment to the player if they are on a team
        if (team != null) {
            SpawnService.giveSpawnEquipment(ctx.config, listOf(player))
        }
    }

    fun init() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            val ctx = Bingo.getContext(player.server)

            if (ctx != null) {
                updateGameMode(ctx, player)

                // if in pregame, move to the lobby world
                if (ctx.state.state == GameState.PREGAME) {
                    val spawnPos = ctx.state.lobbySpawnPos.toBlockPos()
                    player.setSpawnPoint(ctx.lobbyWorld.registryKey, spawnPos, 180f, true, false)
                    player.teleport(ctx.lobbyWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, 180f, 0f)
                }
            }
        }

        ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ ->
            val ctx = Bingo.getContext(player.server)

            // if in pregame, move to the lobby world
            if (ctx != null && ctx.state.state == GameState.PREGAME) {
                val spawnPos = ctx.state.lobbySpawnPos.toBlockPos()
                player.setSpawnPoint(ctx.lobbyWorld.registryKey, spawnPos, 180f, true, false)
                player.teleport(ctx.lobbyWorld, spawnPos.x + 0.5, spawnPos.y.toDouble(), spawnPos.z + 0.5, 180f, 0f)
            }

            // if playing, give the player new spawn equipment
            if (
                ctx != null
                && ctx.state.state == GameState.PLAYING
                && ctx.config.refillPlayerKitOnRespawn
                // if KEEP_INVENTORY is on, we don't need to replace these items
                && !ctx.server.gameRules.get(GameRules.KEEP_INVENTORY).get()
            ) {
                SpawnService.giveSpawnEquipment(ctx.config, listOf(player))
            }
        }

        // Transition from PREGAME -> PLAYING
        GameState.PLAYING.onEnter { prevState ->
            if (prevState != GameState.PREGAME) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(this, player)
            }
        }

        // Transition from PLAYING -> POSTGAME
        GameState.POSTGAME.onEnter { prevState ->
            if (prevState != GameState.PLAYING) return@onEnter

            for (player in server.playerManager.playerList) {
                updateGameMode(this, player)

                // remove any existing maps in the player's inventory
                for (i in 0 until player.inventory.size()) {
                    if (MapItemService.isMapItem(player.inventory.getStack(i)))
                        player.inventory.removeStack(i)
                }

                // give all maps for active teams to the player
                giveSpectatorMapItems(this, player)
            }
        }

        Bingo.onGameTick {
            for (player in server.playerManager.playerList) {
                if (state.state == GameState.PLAYING && player.isAlive) {
                    // give player a new map, if they don't have one?
                    giveMapCardItem(this, player)

                    // if elytra mode is configured, give the player an elytra
                    if (config.isElytra)
                        giveElytra(player)
                }

                if (player.isSpectator || state.state == GameState.POSTGAME)
                    giveSpectatorMapItems(this, player)
            }
        }
    }

}