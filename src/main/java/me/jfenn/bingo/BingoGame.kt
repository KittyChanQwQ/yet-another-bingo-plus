package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.TimeLimit
import me.jfenn.bingo.card.WinCondition
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.Config
import me.jfenn.bingo.config.config
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.menu.ButtonEntity
import me.jfenn.bingo.menu.ItemDistributionEntity
import me.jfenn.bingo.menu.MenuEntity
import me.jfenn.bingo.menu.TeamEntity
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ItemCommand
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import java.time.Duration
import java.time.Instant
import java.util.*

object BingoGame {

    var config: BingoConfig by config("bingo/config.json")

    var state = GameState.PREGAME

    private var leadingTeam: BingoTeam? = null

    private val timeBar by lazy {
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

    fun spawnMenu(pos: Vec3d) {
        val startButton = ButtonEntity(
            position = pos.add(9.0, -1.0, 0.0),
            text = "START",
            isActive = config.isValid(),
        ).apply {
            onClick {
                if (config.isValid())
                    start()
            }
        }

        MenuEntity(
            title = "Win Condition",
            options = WinCondition.values()
                .map { it.name.replace('_', ' ') },
            selectedIndex = WinCondition.values()
                .indexOfFirst { it == config.winCondition },
            position = pos,
        ).onChange {
            config.winCondition = WinCondition.values()[it]
        }

        MenuEntity(
            title = "Time Limit",
            options = TimeLimit.values()
                .map { it.name.replace('_', ' ').trim() },
            selectedIndex = TimeLimit.values()
                .indexOfFirst { it == config.timeLimit },
            position = pos.add(2.0, 0.0, 0.0),
        ).onChange {
            config.timeLimit = TimeLimit.values()[it]
        }

        ItemDistributionEntity(
            position = pos.add(5.5, 0.0, 0.0),
            itemDistribution = config.itemDistribution,
        ).onChange {
            config.itemDistribution = it
            startButton.isActive = config.isValid()
        }

        TeamEntity(
            team = BingoTeam.ORANGE,
            position = Vec3d(-16.5, 4.0, -3.5),
        )

        TeamEntity(
            team = BingoTeam.GREEN,
            position = Vec3d(-14.5, 8.0, -19.5),
        )

        TeamEntity(
            team = BingoTeam.PINK,
            position = Vec3d(5.5, 5.0, -15.5),
        )

        TeamEntity(
            team = BingoTeam.BLUE,
            position = Vec3d(16.5, 3.0, -8.5),
        )

        TeamEntity(
            team = BingoTeam.YELLOW,
            position = Vec3d(18.5, 3.0, 6.5),
        )

        TeamEntity(
            team = BingoTeam.AQUA,
            position = Vec3d(16.5, 5.0, 17.5),
        )

        TeamEntity(
            team = BingoTeam.GRAY,
            position = Vec3d(-1.5, 1.0, 18.5),
        )
    }

    fun rollCard() {
        card = CardService.generate(config.itemDistribution)

        // update the card map images
        for (team in BingoTeam.values()) {
            BingoMapService.update(team, card)
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

        // spread player teams to spawnpoint
        teamSpawnpoints = SpreadPlayers.spread()
        SpreadPlayers.teleportPlayers(teamSpawnpoints)

        // unload lobby dimension
        Bingo.lobbyWorld.chunkManager.removePersistentTickets()

        // reset world settings
        minecraftServer.overworld.timeOfDay = 0

        for (player in minecraftServer.playerManager.playerList) {
            // reset player info
            player.clearStatusEffects()
            player.inventory.clear()

            // unlock all crafting recipes
            // TODO: conditional based on config?
            player.unlockRecipes(minecraftServer.recipeManager.values())

            updateGameMode(player)
            // give each player their team's card
            giveMapCardItem(player)
        }
    }

    fun end() {
        val winMessage = config.winCondition.getWinner(card)?.let { winner ->
                Text.literal("${winner.name} team completed their card!")
                    .formatted(Formatting.BOLD)
                    .formatted(winner.formatting)
            }
            ?: config.winCondition.getLeading(card)?.let { leader ->
                Text.literal("Out of time! ${leader.name} team is the winner!")
                    .formatted(Formatting.BOLD)
                    .formatted(leader.formatting)
            }
            ?: run {
                Text.literal("The game is a draw!")
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

    fun giveMapCardItem(player: ServerPlayerEntity) {
        if (state != GameState.PLAYING) return
        val hasMap = player.inventory.containsAny { it.item == Items.FILLED_MAP }
                || player.currentScreenHandler.cursorStack.item == Items.FILLED_MAP

        if (!hasMap) {
            BingoTeam.fromPlayer(player)
                ?.let { team -> BingoMapService.createMapItem(team) }
                ?.also { player.giveItemStack(it) }
        }
    }

    fun markTileCollected(tile: BingoCardEntry, team: BingoTeam) {
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
        BingoMapService.update(team, card)

        // check if the team has won the game
        if (config.winCondition.getWinner(card) != null) {
            end()
            return
        }

        // check if the leading team has changed, and announce the leader
        // TODO: (green) BLUE team is in the lead!
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

    fun updateTimer() {
        if (state != GameState.PLAYING) return
        val start = startedAt ?: return

        val duration = Duration.between(start, Instant.now())

        for (player in minecraftServer.playerManager.playerList) {
            timeBar.addPlayer(player)
        }

        val secondsRemaining = Duration.ofSeconds(config.timeLimit.seconds - duration.toSeconds())
        val secondsRemainingStr = String.format(Locale.US, "%02dh %02dm %02ds", secondsRemaining.toHoursPart(), secondsRemaining.toMinutesPart(), secondsRemaining.toSecondsPart())

        timeBar.name = Text.literal("Time Remaining: $secondsRemainingStr")
        timeBar.value = ((1f - (duration.toSeconds() / config.timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()

        if (duration.toMinutes() >= config.timeLimit.minutes)
            end()
    }

}