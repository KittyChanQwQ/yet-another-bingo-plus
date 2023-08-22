package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.config
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.formatString
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode
import net.minecraft.world.GameRules
import java.time.Duration
import java.time.Instant

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
        // if the game has already been started, ignore
        if (startedAt != null) return

        // save the current config, if overwrite option is set
        if (config.overwrite) {
            config = config.copy()
        }

        state = GameState.PLAYING
        startedAt = Instant.now()
        rollCard()

        // update server to match config choices
        minecraftServer.isPvpEnabled = config.isPvpEnabled
        minecraftServer.gameRules.get(GameRules.KEEP_INVENTORY).set(config.isKeepInventory, minecraftServer)

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
                Text.empty()
                    .append(winner.text())
                    .append(Text.literal(" completed their card!").formatted(Formatting.YELLOW))
            }
            ?: config.winCondition.getLeading(card)?.let { leader ->
                Text.empty()
                    .append(Text.literal("$reason ").formatted(Formatting.YELLOW))
                    .append(leader.text())
                    .append(Text.literal(" is the winner!").formatted(Formatting.YELLOW))
            }
            ?: Text.literal("$reason The game is a draw!").formatted(Formatting.YELLOW)

        sequence {
            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
            yield(Text.empty().append("  ").append(winMessage))

            Text.empty()
                .append(Text.literal("  Game duration: ").formatted(Formatting.YELLOW))
                .append(Duration.between(startedAt ?: Instant.now(), Instant.now()).formatString())
                .also { yield(it) }

            Text.empty()
                .append(Text.literal("  Seed: "))
                .append(Texts.bracketedCopyable(minecraftServer.overworld.seed.toString()))
                .also { yield(it) }

            yield(Text.literal("┄".repeat(50)).formatted(Formatting.YELLOW))
        }.forEach {
            minecraftServer.playerManager.broadcast(it, false)
        }

        state = GameState.POSTGAME
        for (player in minecraftServer.playerManager.playerList) {
            // send the winning team title message
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO")))
            player.networkHandler.sendPacket(SubtitleS2CPacket(winMessage))

            updateGameMode(player)

            Sounds.playGameOver(player)

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

        val updatedTeams = mutableSetOf<BingoTeam>()

        for (player in minecraftServer.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player) ?: continue

            // check inventory for new collected items
            for (stack in player.allHeldStacks()) {
                // get the item tile, if it is on the card
                val tile = card.itemIndex[stack.item] ?: continue

                // if the item hasn't been achieved and can be satisfied, mark it as collected
                if (!tile.hasAchieved(team) && tile.canSatisfy(stack)) {
                    markTileCollected(tile, team)
                    updatedTeams.add(team)
                }
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
                    if (!hasTeamItem) {
                        markTileLost(item, team)
                        updatedTeams.add(team)
                    }
                }
            }
        }

        // if there are no updated teams, we can skip the remaining checks...
        if (updatedTeams.isEmpty()) return

        // update the team map(s)
        if (config.gameMode == BingoMode.LOCKOUT) {
            // if the mode is LOCKOUT, all team cards need to be updated (to redraw locked items)
            for (team in BingoTeam.values())
                BingoMapService.update(team, card, config)
        } else {
            // otherwise, only redraw maps for the updated teams
            for (team in updatedTeams)
                BingoMapService.update(team, card, config)
        }

        // check if a team has won the game
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
                Text.empty()
                    .append(leadingTeam.text())
                    .append(Text.literal(" is in the lead!").formatted(Formatting.YELLOW))
            } else {
                val team = updatedTeams.first()
                Text.empty()
                    .append(team.text())
                    .append(Text.literal(" is tied in the lead!").formatted(Formatting.YELLOW))
            }

            minecraftServer.playerManager.broadcast(leadingMessage, false)
        }
    }

    private fun markTileCollected(tile: BingoCardEntry, team: BingoTeam) {
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
                Text.empty()
                    .append(team.text())
                    .append(Text.literal(" has scored $linesAfter line${if (linesAfter > 1) "s" else ""}!").formatted(Formatting.YELLOW)),
                false,
            )
        }

        // play the doink sound
        Sounds.playTileCollected(team)
    }

    /**
     * This is only used in BingoMode.INVENTORY to un-mark a tile if
     * the item is no longer in a player's inventory
     */
    private fun markTileLost(tile: BingoCardEntry, team: BingoTeam) {
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

        timeBar.name = Text.empty()
            .append(Text.literal("Time Remaining: "))
            .append(
                Text.literal(secondsRemaining.formatString())
                    .formatted(if (secondsRemaining.toSeconds() <= 30 && secondsRemaining.toSeconds() % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
            )
            .append(Text.literal(" or ${config.winCondition.name.lowercase().replace('_', ' ')}"))

        timeBar.value = ((1f - (duration.toSeconds() / config.timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()

        if (duration.toMinutes() >= config.timeLimit.minutes)
            end("Out of time!")
    }

}