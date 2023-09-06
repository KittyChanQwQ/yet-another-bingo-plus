package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.CardService
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.config
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.formatString
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.command.GiveCommand
import net.minecraft.server.command.ItemCommand
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
            MapRenderService.update(team, card, config)
        }
    }

    fun start() {
        // if the game has already been started, ignore
        if (startedAt != null) return
        if (!config.isValid()) return

        // save the current config, if overwrite option is set
        if (config.overwrite) {
            config = config.copy()
        }

        state = GameState.PLAYING
        startedAt = Instant.now()
        rollCard()

        for (player in minecraftServer.playerManager.playerList) {
            val team = BingoTeam.fromPlayer(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game is starting!")))
            Sounds.playGameStarted(player)
        }

        // update server to match config choices
        minecraftServer.isPvpEnabled = config.isPvpEnabled
        minecraftServer.gameRules.get(GameRules.KEEP_INVENTORY).set(config.isKeepInventory, minecraftServer)
        minecraftServer.gameRules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(true, minecraftServer)
        minecraftServer.gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(true, minecraftServer)

        // spread player teams to spawnpoint
        val spread = SpreadPlayers(world = minecraftServer.overworld)
        teamSpawnpoints = spread.spread(config.spreadPlayersDistance) // TODO: configurable spawn distance?
        spread.teleportPlayers(teamSpawnpoints)

        // unload lobby dimension
        lobbyWorld.chunkManager.removePersistentTickets()

        // reset world settings
        minecraftServer.overworld.timeOfDay = 0

        for (player in minecraftServer.playerManager.playerList) {
            // reset player info
            player.clearStatusEffects()
            player.inventory.clear()
            player.fireTicks = 0
            player.isOnFire = false
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

            val team = BingoTeam.fromPlayer(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game has started!")))
            Sounds.playGameStarted(player)
        }
    }

    fun end(reason: String? = null) {
        val winMessage = card.getWinner(config)?.let { winner ->
                Text.empty()
                    .append(winner.text())
                    .append(Text.literal(" completed their card!").formatted(Formatting.YELLOW))
            }
            ?: card.getLeadingByLines()?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text())
                    .append(Text.literal(" had more lines and won!").formatted(Formatting.YELLOW))
            }
            ?: card.getLeadingByItems()?.let { leader ->
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(leader.text())
                    .append(Text.literal(" had more items and won!").formatted(Formatting.YELLOW))
            }
            ?: run {
                Text.empty()
                    .append(Text.literal(reason?.plus(" ") ?: "").formatted(Formatting.YELLOW))
                    .append(Text.literal("The game is a draw!").formatted(Formatting.YELLOW))
            }

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
                if (MapItemService.isMapItem(player.inventory.getStack(i)))
                    player.inventory.removeStack(i)
            }

            // give all maps for active teams to the player
            giveSpectatorMapItems(player)
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

        val hasMap = player.allHeldStacks().any { MapItemService.isMapItem(it) }

        if (!hasMap) {
            BingoTeam.fromPlayer(player)
                ?.let { team -> MapItemService.createMapItem(team) }
                ?.also { player.giveItemStack(it) }
        }
    }

    /**
     * Ensure that the player has every team's bingo map card
     * in their hotbar slots. Indiscriminately replaces any existing
     * hotbar items.
     */
    fun giveSpectatorMapItems(player: ServerPlayerEntity) {
        if (state == GameState.PREGAME) return

        val isOnTeam = BingoTeam.values()
            .any { player.isTeamPlayer(it.team()) }

        if (isOnTeam && state != GameState.POSTGAME) return

        BingoTeam.values()
            .filter { it.team().playerList.isNotEmpty() }
            .map { MapItemService.createMapItem(it) }
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
                if (!tile.canSatisfy(stack)) continue
                if (tile.hasAchieved(team)) continue

                markTileCollected(tile, team)
                    // if the tile has changed, mark updatedTeams
                    .also { isChanged -> if (isChanged) updatedTeams.add(team) }
            }
        }

        // if BingoMode.INVENTORY, we need to check that each team still has all their items
        // and mark them as lost if not
        if (config.gameMode == BingoMode.INVENTORY) {
            for (item in card.entries) {
                for (team in item.teams.toSet()) {
                    val teamInstance = team.team()
                    val hasTeamItem = minecraftServer.playerManager.playerList
                        .filter { it.isTeamPlayer(teamInstance) }
                        .any { player -> player.inventory.containsAny { item.canSatisfy(it) }
                                || item.canSatisfy(player.currentScreenHandler.cursorStack) }

                    // if noone on the team has the item anymore, mark it lost
                    if (!hasTeamItem) {
                        markTileLost(item, team)
                            // if the tile has changed, mark updatedTeams
                            .also { isChanged -> if (isChanged) updatedTeams.add(team) }
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
                MapRenderService.update(team, card, config)
        } else {
            // otherwise, only redraw maps for the updated teams
            for (team in updatedTeams)
                MapRenderService.update(team, card, config)
        }

        // check if a team has won the game
        if (card.getWinner(config) != null) {
            end()
            return
        }

        // check if the game is in a stalemate
        if (card.isStalemate(config)) {
            end("Stalemate!")
            return
        }

        // check if the leading team has changed, and announce the leader
        val leadingTeam = card.getLeading()
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

    /**
     * @return if the given tile has been changed
     */
    private fun markTileCollected(tile: BingoCardEntry, team: BingoTeam): Boolean {
        if (state != GameState.PLAYING) return false
        // if the gamemode is lockout, tiles cannot be collected by >1 team
        if (config.gameMode == BingoMode.LOCKOUT && tile.teams.isNotEmpty())
            return false
        // if the team has already achieved the tile, there is nothing to change
        if (tile.hasAchieved(team))
            return false

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
        return true
    }

    /**
     * This is only used in BingoMode.INVENTORY to un-mark a tile if
     * the item is no longer in a player's inventory
     *
     * @return if the given tile has been changed
     */
    private fun markTileLost(tile: BingoCardEntry, team: BingoTeam): Boolean {
        if (state != GameState.PLAYING) return false
        // if the team doesn't have the tile, there is nothing to change
        if (!tile.hasAchieved(team)) return false

        tile.setAchieved(team, false)
        return true
    }

    fun updateTimer() {
        if (state != GameState.PLAYING) return
        val start = startedAt ?: return

        val timeLimit = config.timerMinutes?.let { Duration.ofMinutes(it.toLong()) }

        val timerText = if (timeLimit != null) {
            val duration = Duration.between(start, Instant.now())
            val secondsRemaining = Duration.ofSeconds(timeLimit.seconds - duration.toSeconds())

            timeBar.value = ((1f - (duration.toSeconds() / timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()

            if (duration.toMinutes() >= timeLimit.toMinutes()) {
                end("Out of time!")
                return
            }

            Text.literal(secondsRemaining.formatString())
                .formatted(Formatting.BOLD, if (secondsRemaining.toSeconds() <= 30 && secondsRemaining.toSeconds() % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
        } else {
            Text.literal("(No Time Limit)")
                .formatted(Formatting.BOLD)
        }

        timeBar.name = Text.empty()
            .append(Text.literal("Time Remaining: "))
            .append(timerText)
            .append(Text.literal(" or "))
            .append(
                Text.literal(config.winCondition.name.lowercase().replace('_', ' '))
                    .formatted(Formatting.BOLD)
            )
    }

}