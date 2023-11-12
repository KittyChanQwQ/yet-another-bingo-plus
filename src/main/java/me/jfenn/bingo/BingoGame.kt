package me.jfenn.bingo

import me.jfenn.bingo.card.*
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.ConfigService
import me.jfenn.bingo.config.config
import me.jfenn.bingo.map.MapItemService
import me.jfenn.bingo.map.MapRenderService
import me.jfenn.bingo.menu.MenuService
import me.jfenn.bingo.spawn.SpawnService
import me.jfenn.bingo.utils.allHeldStacks
import me.jfenn.bingo.utils.formatString
import me.jfenn.bingo.utils.formatTitle
import me.jfenn.bingo.utils.giveOrEquipStack
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardPlayerUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.scoreboard.ScoreboardCriterion
import net.minecraft.scoreboard.ScoreboardDisplaySlot
import net.minecraft.scoreboard.ServerScoreboard
import net.minecraft.server.command.CommandOutput
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
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

    var card: BingoCard = CardService.generate(config)

    private var startedAt: Instant? = null

    fun rollCard() {
        card = CardService.generate(config)

        // update the card map images
        for (team in BingoTeam.entries) {
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
        SpawnService.createSpawnpoints(config)

        // unload lobby dimension
        MenuService.despawn()
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

            updateSettings(player)

            // reset advancements state
            // The yarn mappings for these APIs conflict between versions, so we're using the command for this instead
            // for (advancement in minecraftServer.advancementLoader.advancements) {
            //     val progress = player.advancementTracker
            //     for (criteria in progress.obtainedCriteria) {
            //         player.advancementTracker.revokeCriterion(advancement, criteria)
            //     }
            // }
            minecraftServer.commandManager.executeWithPrefix(
                minecraftServer.commandSource.withOutput(CommandOutput.DUMMY),
                "advancement revoke @a everything"
            )

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

        // give spawn equipment to all players that are on a team
        SpawnService.giveSpawnEquipment(config, minecraftServer.playerManager.playerList.filter { BingoTeam.fromPlayer(it) != null })
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
        val isOnTeam = BingoTeam.entries
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

        val isOnTeam = BingoTeam.entries
            .any { player.isTeamPlayer(it.team()) }

        if (isOnTeam && state != GameState.POSTGAME) return

        BingoTeam.entries
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
            for (item in card.entries.filterIsInstance<ItemEntry>()) {
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
        if (config.gameMode == BingoMode.LOCKOUT || config.drawOtherTeamCompletions) {
            // if the mode is LOCKOUT, all team cards need to be updated (to redraw locked items)
            for (team in BingoTeam.entries)
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

            // if the leading team should be shown, announce the new leader
            if (config.showLeadingTeam) {
                val leadingMessage = if (leadingTeam != null) {
                    val items = card.countItems(leadingTeam)


                    Text.empty()
                        .append(leadingTeam.text())
                        .append(Text.literal(
                            " is in the lead"
                                    + (if (config.showCompletedItems) " with $items item${"s".takeIf { items > 1 }.orEmpty()}" else "")
                                    + "!"
                        ).formatted(Formatting.YELLOW))
                } else {
                    val team = updatedTeams.first()
                    val items = card.countItems(team)

                    Text.empty()
                        .append(team.text())
                        .append(Text.literal(
                            " is tied in the lead"
                                    + (if (config.showCompletedItems) " with $items item${"s".takeIf { items > 1 } ?: ""}" else "")
                                    + "!"
                        ).formatted(Formatting.YELLOW))
                }

                minecraftServer.playerManager.playerList
                    .filter { ConfigService.getPlayer(it.uuidAsString).leadingMessages }
                    .forEach { it.sendMessage(leadingMessage, false) }
            }
        }
    }

    /**
     * @return if the given tile has been changed
     */
    private fun markTileCollected(tile: BingoCardEntry, team: BingoTeam): Boolean {
        if (state != GameState.PLAYING) return false
        // if the gamemode is lockout, tiles cannot be collected by >1 team
        if (config.gameMode == BingoMode.LOCKOUT && tile.hasAnyAchieved())
            return false
        // if the team has already achieved the tile, there is nothing to change
        if (tile.hasAchieved(team))
            return false

        // unlock the collected item
        val linesBefore = card.countLines(team)
        tile.setAchieved(team, true)
        val linesAfter = card.countLines(team)

        if (linesAfter > linesBefore && config.showCompletedLines) {
            val message = Text.empty()
                .append(team.text())
                .append(Text.literal(" has scored $linesAfter line${if (linesAfter > 1) "s" else ""}!").formatted(Formatting.YELLOW))

            minecraftServer.playerManager.playerList
                .filter { ConfigService.getPlayer(it.uuidAsString).scoreMessages }
                .forEach { it.sendMessage(message, false) }
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

    private fun formatTimeRemaining(): Text {
        val timeLimit = config.timerMinutes?.let { Duration.ofMinutes(it.toLong()) }
            ?: return Text.literal("(No Time Limit)").formatted(Formatting.BOLD)

        val start = startedAt ?: return Text.literal(timeLimit.formatString())
        val duration = Duration.between(start, Instant.now())
        val secondsRemaining = Duration.ofSeconds(timeLimit.seconds - duration.toSeconds())

        if (duration.toMinutes() >= timeLimit.toMinutes() || state == GameState.POSTGAME)
            return Text.literal("(Game Over)").formatted(Formatting.BOLD)

        return Text.literal(secondsRemaining.formatString())
            .formatted(Formatting.BOLD, if (secondsRemaining.toSeconds() <= 30 && secondsRemaining.toSeconds() % 2 == 0L) { Formatting.RED } else { Formatting.WHITE })
    }

    fun updateTimer() {
        if (state != GameState.PLAYING) return
        val start = startedAt ?: return
        val timeLimit = config.timerMinutes?.let { Duration.ofMinutes(it.toLong()) }
            ?: return

        val duration = Duration.between(start, Instant.now())

        if (config.showRemainingTime) {
            timeBar.value = ((1f - (duration.toSeconds() / timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()
        }

        val isTimeOver = duration.toMinutes() > timeLimit.toMinutes()
        if (isTimeOver) {
            end("Out of time!")
        }
    }

    private val teamScoreboards by lazy {
        BingoTeam.entries
            .toMutableList<BingoTeam?>()
            .apply { add(null) }
            .associateWith { team ->
                minecraftServer.scoreboard.addObjective(
                    "bingo_sidebar_" + team?.name,
                    ScoreboardCriterion.DUMMY,
                    Text.literal("Minecraft BINGO"),
                    ScoreboardCriterion.RenderType.INTEGER,
                )
            }
    }

    fun updateSidebars() {
        timeBar.name = Text.empty()
            .let {
                if (config.showRemainingTime) {
                    it.append(Text.literal("Time Remaining: "))
                        .append(formatTimeRemaining())
                        .append(Text.literal(" or "))
                } else it
            }
            .append(
                Text.literal(config.winCondition.name.formatTitle())
                    .formatted(Formatting.BOLD)
            )

        for ((team, objective) in teamScoreboards) {
            val scoreboardText = buildList {
                add("")
                add("Game mode: ${Formatting.YELLOW}${config.gameMode.name.formatTitle()}")
                add("Win condition: ${Formatting.YELLOW}${config.winCondition.name.formatTitle()}")
                if (config.showRemainingTime) {
                    add("Time left: ${Formatting.YELLOW}${formatTimeRemaining().string}")
                }
                add("")
                if (team != null) {
                    add("Team: ${team.formatting}${team.name}")
                    add("Items scored: ${team.formatting}${card.countItems(team)}")
                    add("Lines scored: ${team.formatting}${card.countLines(team)}")
                    add("")
                }
                if (config.showLeadingTeam) {
                    add("Leading team: ${leadingTeam?.let { it.formatting.toString() + it.name } ?: "${Formatting.WHITE}Tie"}")
                }

                val formatting = when {
                    // if the leading team should be hidden, don't use its formatting
                    !config.showLeadingTeam -> Formatting.WHITE
                    // otherwise, default to WHITE if the team is tied
                    else -> leadingTeam?.formatting ?: Formatting.WHITE
                }

                if (config.showCompletedItems) {
                    // if the leading team is a tie, find one of the tied teams to get the item/line count
                    val leadingOrTied = leadingTeam ?: card.getLeadingByItems(allowTies = true)
                    val itemsScored = leadingOrTied?.let { card.countItems(it)} ?: 0
                    add("Items scored: $formatting$itemsScored")
                }
                if (config.showCompletedLines) {
                    // if the leading team is a tie, find one of the tied teams to get the item/line count
                    val leadingOrTied = leadingTeam ?: card.getLeadingByLines(allowTies = true)
                    val linesScored = leadingOrTied?.let { card.countLines(it)} ?: 0
                    add("Lines scored: $formatting$linesScored")
                }

                if (config.showLeadingTeam || config.showCompletedItems || config.showCompletedLines)
                    add("")
            }.mapIndexed { i, str ->
                str.ifEmpty { " ".repeat(i) }
            }

            // Remove any previous sidebar lines that have been changed
            for (score in minecraftServer.scoreboard.getAllPlayerScores(objective)) {
                if (!scoreboardText.contains(score.playerName)) {
                    minecraftServer.scoreboard.resetPlayerScore(score.playerName, objective)
                    // The server sometimes fails to send this packet, so we're adding it manually. I don't know why.
                    // - yes, this does need to be sent to all players, otherwise it may cause desync
                    minecraftServer.playerManager.sendToAll(ScoreboardPlayerUpdateS2CPacket(ServerScoreboard.UpdateMode.REMOVE, objective.name, score.playerName, 0))
                }
            }

            // Set the score of any new sidebar lines to the list index
            scoreboardText.forEachIndexed { i, text ->
                val score = minecraftServer.scoreboard.getPlayerScore(text, objective)
                score.score = scoreboardText.size - i - 1
            }

            // Manually send the scoreboard packets to each player
            val packets = minecraftServer.scoreboard.createChangePackets(objective)
            val displayPacket = ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, objective)
            val hidePacket = ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null)
            minecraftServer.playerManager.playerList
                .filter { BingoTeam.fromPlayer(it) == team }
                .filter { ConfigService.getPlayer(it.uuidAsString).scoreboard }
                .forEach { player ->
                    // if the player is holding the map item, show the scoreboard; otherwise, hide it
                    val isHoldingMapItem = player.interactionManager.gameMode != GameMode.SURVIVAL
                            || MapItemService.isMapItem(player.mainHandStack)
                            || MapItemService.isMapItem(player.offHandStack)

                    if (isHoldingMapItem) {
                        for (packet in packets)
                            player.networkHandler.sendPacket(packet)

                        player.networkHandler.sendPacket(displayPacket)
                    } else {
                        player.networkHandler.sendPacket(hidePacket)
                    }
                }
        }
    }

    fun updateSettings(player: ServerPlayerEntity) {
        val config = ConfigService.getPlayer(player.uuidAsString)

        if (config.bossbar)
            timeBar.addPlayer(player)
        else timeBar.removePlayer(player)

        if (!config.scoreboard) {
            // send a packet remove the scoreboard sidebar if set to false
            player.networkHandler.sendPacket(ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, null))
        }
    }

}