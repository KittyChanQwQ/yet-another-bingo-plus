package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.config
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.allHeldStacks
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode
import java.time.Duration
import java.time.Instant
import java.util.*

object BingoGame {

    var config: BingoConfig by config("bingo/config.json")

    var state = GameState.PREGAME
        set(value) {
            minecraftServer.setMotd("${value.color}[${value.motd}]§f Minecraft BINGO")
            field = value
        }

    private var leadingTeam: BingoTeam? = null

    val timeBar by lazy {
        val id = Identifier.of("fennifith", "bingo")

        // if the boss bar already exists, recreate it
        minecraftServer.bossBarManager.get(id)
            ?.also { minecraftServer.bossBarManager.remove(it) }

        minecraftServer.bossBarManager.add(id, Text.literal("Time Remaining"))
            .apply {
                color = BossBar.Color.WHITE
                style = BossBar.Style.PROGRESS
                value = 0
                maxValue = 10_000
            }
    }

    var card: BingoCard = CardService.generate(config.itemDistribution)

    var teamSpawnpoints = mapOf<BingoTeam, BlockPos>()

    private var startedAt: Instant? = null

    fun rollCard() {
        card = CardService.generate(config.itemDistribution)

        // update the card map images
        for (team in BingoTeam.values()) {
            BingoMapService.update(team, card, config)
        }
    }

    fun start() {
        // save the current config, if overwrite option is set
        if (config.overwrite) {
            config = config.copy()
        }

        state = GameState.PLAYING
        startedAt = Instant.now()
        rollCard()

        // update server to match config choices
        minecraftServer.isPvpEnabled = config.isPvpEnabled

        // spread player teams to spawnpoint
        val spread = SpreadPlayers(world = minecraftServer.overworld)
        teamSpawnpoints = spread.spread(500) // TODO: configurable spawn distance?
        spread.teleportPlayers(teamSpawnpoints)

        // unload lobby dimension
        lobbyWorld.chunkManager.removePersistentTickets()

        // reset world settings
        minecraftServer.overworld.timeOfDay = 0

        for (player in minecraftServer.playerManager.playerList) {
            // reset player info
            player.clearStatusEffects()
            player.inventory.clear()
            player.health = 20f
            player.hungerManager.foodLevel = 20
            player.hungerManager.saturationLevel = 5f
            player.hungerManager.exhaustion = 0f

            // add player to boss bar
            timeBar.addPlayer(player)

            // reset advancements state
            for (advancement in minecraftServer.advancementLoader.advancements) {
                val progress = player.advancementTracker.getProgress(advancement)
                for (criteria in progress.obtainedCriteria) {
                    player.advancementTracker.revokeCriterion(advancement, criteria)
                }
            }

            // unlock all crafting recipes, if set
            if (config.isUnlockRecipes)
                player.unlockRecipes(minecraftServer.recipeManager.values())

            updateGameMode(player)
            // give each player their team's card
            giveMapCardItem(player)
        }
    }

    fun end(reason: String) {
        val winMessage = config.winCondition.getWinner(card)?.let { winner ->
                Text.literal("${winner.name} team completed their card!")
                    .formatted(Formatting.BOLD)
                    .formatted(winner.formatting)
            }
            ?: config.winCondition.getLeading(card)?.let { leader ->
                Text.literal("$reason ${leader.name} team is the winner!")
                    .formatted(Formatting.BOLD)
                    .formatted(leader.formatting)
            }
            ?: run {
                Text.literal("$reason The game is a draw!")
                    .formatted(Formatting.BOLD)
            }

        minecraftServer.playerManager.broadcast(winMessage, false)

        state = GameState.POSTGAME
        for (player in minecraftServer.playerManager.playerList) {
            // send the winning team title message
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO")))
            player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))

            updateGameMode(player)

            player.networkHandler.sendPacket(
                PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_PORTAL_TRAVEL),
                    SoundCategory.BLOCKS,
                    player.x, player.y, player.z,
                    0.2f, 1f, 0
                )
            )

            // hide the boss bar once the game is complete
            timeBar.removePlayer(player)

            // remove any existing maps in the player's inventory
            for (i in 0 until player.inventory.size()) {
                if (BingoMapService.isMapItem(player.inventory.getStack(i)))
                    player.inventory.removeStack(i)
            }

            // give all maps for active teams to the player
            BingoTeam.values()
                .filter { it.team().playerList.isNotEmpty() }
                .map { BingoMapService.createMapItem(it) }
                .forEachIndexed { i, stack ->
                    player.getStackReference(i).set(stack)
                    player.currentScreenHandler.sendContentUpdates()
                }
        }
    }

    fun updateGameMode(player: ServerPlayerEntity) {
        val isOnTeam = BingoTeam.values()
            .any { player.isTeamPlayer(it.team()) }

        val gameMode = when {
            state == GameState.PREGAME -> GameMode.ADVENTURE
            state == GameState.PLAYING && isOnTeam -> GameMode.SURVIVAL
            state == GameState.POSTGAME -> GameMode.CREATIVE
            else -> GameMode.SPECTATOR
        }

        player.changeGameMode(gameMode)
    }

    /**
     * Ensure that the player has their team's bingo map card.
     * Has no effect if the player already has a card
     */
    fun giveMapCardItem(player: ServerPlayerEntity) {
        if (state != GameState.PLAYING) return
        if (!player.isAlive) return

        val hasMap = player.allHeldStacks().any { BingoMapService.isMapItem(it) }

        if (!hasMap) {
            BingoTeam.fromPlayer(player)
                ?.let { team -> BingoMapService.createMapItem(team) }
                ?.also { player.giveItemStack(it) }
        }
    }

    /**
     * Equip the player with elytra and firework rockets.
     * Only runs if config.isElytra is true
     */
    fun giveElytra(player: ServerPlayerEntity) {
        if (state != GameState.PLAYING) return
        if (BingoTeam.fromPlayer(player) == null) return
        if (!player.isAlive) return
        if (!config.isElytra) return

        val ARMOR_SLOT = 2
        val hasElytra = player.inventory.getArmorStack(ARMOR_SLOT).item == Items.ELYTRA

        if (!hasElytra) {
            // insert elytra in armor slot
            player.inventory.armor[ARMOR_SLOT] = ItemStack(Items.ELYTRA, 1).also {
                it.getOrCreateNbt().apply {
                    // unbreakable (should not take damage)
                    putBoolean("Unbreakable", true)
                    put("Enchantments", NbtList().apply {
                        // curse of binding
                        add(NbtCompound().apply {
                            putString("id", "binding_curse")
                            putInt("lvl", 1)
                        })
                    })
                    putBoolean("bingo_ignore", true)
                }
            }
        }

        // also give rocket
        val rocketStack = player.allHeldStacks().find { it.item == Items.FIREWORK_ROCKET }

        if (rocketStack != null && rocketStack.count < 2) {
            rocketStack.count = 2
        }

        if (rocketStack == null) {
            player.giveItemStack(ItemStack(Items.FIREWORK_ROCKET, 2).also {
                it.getOrCreateNbt().apply {
                    putBoolean("bingo_ignore", true)
                    put("Fireworks", NbtCompound())
                }
            })
        }
    }

    fun updateInventory() {
        if (state != GameState.PLAYING) return
        // TODO: only update the inventory state when marked dirty

        for (player in minecraftServer.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player) ?: continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = card.itemIndex[stack.item] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (!tile.hasAchieved(team) && tile.canSatisfy(stack))
                    markTileCollected(tile, team)
            }
        }

        // if BingoMode.INVENTORY, we need to check that each team still has all their items
        // and mark them as lost if not
        if (config.gameMode == BingoMode.INVENTORY) {
            for (item in card.entries) {
                for (team in item.teams) if (item.hasAchieved(team)) {
                    val teamInstance = team.team()
                    val hasTeamItem = minecraftServer.playerManager.playerList
                        .filter { it.isTeamPlayer(teamInstance) }
                        .any { player -> player.inventory.containsAny { item.canSatisfy(it) }
                                || item.canSatisfy(player.currentScreenHandler.cursorStack) }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) markTileLost(item, team)
                }
            }
        }
    }

    fun markTileCollected(tile: BingoCardEntry, team: BingoTeam) {
        if (state != GameState.PLAYING) return
        // if the gamemode is lockout, tiles cannot be collected by >1 team
        if (config.gameMode == BingoMode.LOCKOUT && tile.teams.isNotEmpty())
            return

        // unlock the collected item
        val linesBefore = card.countLines(team)
        tile.setAchieved(team, true)
        val linesAfter = card.countLines(team)

        if (linesAfter > linesBefore) {
            minecraftServer.playerManager.broadcast(
                Text.literal("${team.name} team has scored $linesAfter line${if (linesAfter > 1) "s" else ""}!")
                    .formatted(Formatting.BOLD)
                    .formatted(team.formatting),
                false,
            )
        }

        // play the doink sound
        playTileCollected(team)

        // update the team map
        if (config.gameMode == BingoMode.LOCKOUT) {
            // if the mode is LOCKOUT, all team cards need to be updated (to redraw locked items)
            for (otherTeam in BingoTeam.values())
                BingoMapService.update(otherTeam, card, config)
        } else {
            BingoMapService.update(team, card, config)
        }

        // check if the team has won the game
        if (config.winCondition.getWinner(card) != null) {
            end("")
            return
        }

        // check if the game is in a stalemate
        if (config.winCondition.isStalemate(card, config)) {
            end("Stalemate!")
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = config.winCondition.getLeading(card)
        if (leadingTeam != this.leadingTeam) {
            this.leadingTeam = leadingTeam

            val leadingMessage = if (leadingTeam != null) {
                Text.literal("${leadingTeam.name} team is in the lead!")
                    .formatted(Formatting.BOLD)
                    .formatted(leadingTeam.formatting)
            } else {
                Text.literal("${team.name} team is tied in the lead!")
                    .formatted(Formatting.BOLD)
                    .formatted(team.formatting)
            }

            minecraftServer.playerManager.broadcast(leadingMessage, false)
        }
    }

    fun playTileCollected(team: BingoTeam) {
        for (player in minecraftServer.playerManager.playerList) {
            if (player.isTeamPlayer(team.team())) {
                player.networkHandler.sendPacket(
                    PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP),
                        SoundCategory.BLOCKS,
                        player.x, player.y, player.z,
                        1f, 2f, 0
                    )
                )
            } else {
                player.networkHandler.sendPacket(
                    PlaySoundS2CPacket(
                        Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value()),
                        SoundCategory.BLOCKS,
                        player.x, player.y, player.z,
                        1f, 1f, 0
                    )
                )
            }
        }
    }

    /**
     * This is only used in BingoMode.INVENTORY to un-mark a tile if
     * the item is no longer in a player's inventory
     */
    fun markTileLost(tile: BingoCardEntry, team: BingoTeam) {
        if (state != GameState.PLAYING) return
        if (config.gameMode != BingoMode.INVENTORY) return

        tile.setAchieved(team, false)
        BingoMapService.update(team, card, config)
    }

    fun updateTimer() {
        if (state != GameState.PLAYING) return
        val start = startedAt ?: return

        val duration = Duration.between(start, Instant.now())

        val secondsRemaining = Duration.ofSeconds(config.timeLimit.seconds - duration.toSeconds())
        val secondsRemainingStr = String.format(Locale.US, "%02dm %02ds", secondsRemaining.toMinutesPart(), secondsRemaining.toSecondsPart())

        timeBar.name = Text.empty()
            .append(Text.literal("Time Remaining: "))
            .append(
                Text.literal("${secondsRemaining.toHoursPart().takeIf { it > 0 }?.let { "${it}h " }.orEmpty()}$secondsRemainingStr")
                    .formatted(if (secondsRemaining.toSeconds() <= 30 && secondsRemaining.toSeconds() % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
            )
            .append(Text.literal(" or ${config.winCondition.name.lowercase().replace('_', ' ')}"))

        timeBar.value = ((1f - (duration.toSeconds() / config.timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()

        if (duration.toMinutes() >= config.timeLimit.minutes)
            end("Out of time!")
    }

}