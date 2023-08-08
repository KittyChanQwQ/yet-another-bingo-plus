package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.TimeLimit
import me.jfenn.bingo.card.WinCondition
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.menu.ButtonEntity
import me.jfenn.bingo.menu.ItemDistributionEntity
import me.jfenn.bingo.menu.MenuEntity
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.entity.boss.BossBar
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
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

    var state = GameState.PREGAME

    var winCondition = WinCondition.THREE_LINES

    var timeLimit = TimeLimit.ONE_HOUR
    val timeBar by lazy {
        // if the boss bar already exists, recreate it
        minecraftServer.bossBarManager.get(Identifier.of("fennifith", "bingo"))
            ?.also { minecraftServer.bossBarManager.remove(it) }

        minecraftServer.bossBarManager.add(Identifier.of("fennifith", "bingo"), Text.literal("Time Remaining"))
            .apply {
                color = BossBar.Color.WHITE
                style = BossBar.Style.PROGRESS
                value = 0
                maxValue = 10_000
            }
    }

    var itemDistribution = listOf(2, 6, 9, 6, 2)

    var card: BingoCard = CardService.generate(itemDistribution)

    var teamSpawnpoints = mapOf<BingoTeam, BlockPos>()

    var startedAt: Instant? = null

    fun spawnMenu() {
        MenuEntity(
            title = "Win Condition",
            options = WinCondition.values()
                .map { it.name.replace('_', ' ') },
            selectedIndex = WinCondition.values()
                .indexOfFirst { it == winCondition },
            position = Vec3d(0.0, 80.0, 0.0),
        ).onChange {
            winCondition = WinCondition.values()[it]
        }

        MenuEntity(
            title = "Time Limit",
            options = TimeLimit.values()
                .map { it.name.replace('_', ' ').trim() },
            selectedIndex = TimeLimit.values()
                .indexOfFirst { it == timeLimit },
            position = Vec3d(2.0, 80.0, 0.0),
        ).onChange {
            timeLimit = TimeLimit.values()[it]
        }

        ItemDistributionEntity(
            position = Vec3d(5.5, 80.0, 0.0),
        ).onChange {
            itemDistribution = it
        }

        ButtonEntity(
            position = Vec3d(9.0, 80.0, 0.0),
            text = "START",
        ).onClick {
            start()
        }
    }

    fun start() {
        card = CardService.generate(itemDistribution)
        state = GameState.PLAYING
        startedAt = Instant.now()

        // create the initial card map images
        for (team in BingoTeam.values()) {
            BingoMapService.update(team, card)
        }

        // spread player teams to spawnpoint
        teamSpawnpoints = SpreadPlayers.spread()
        SpreadPlayers.teleportPlayers(teamSpawnpoints)

        minecraftServer.playerManager.playerList.forEach { player ->
            // reset player info
            player.clearStatusEffects()
            player.inventory.clear()

            // unlock all crafting recipes
            // TODO: conditional based on config
            player.unlockRecipes(minecraftServer.recipeManager.values())

            updateGameMode(player)
            // give each player their team's card
            giveMapCardItem(player)
        }
    }

    fun end() {
        state = GameState.POSTGAME
        minecraftServer.playerManager.playerList.forEach { player ->
            updateGameMode(player)

            player.networkHandler.sendPacket(
                PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_PORTAL_TRAVEL),
                    SoundCategory.BLOCKS,
                    player.x, player.y, player.z,
                    0.2f, 1f, 0
                )
            )
        }
    }

    fun updateGameMode(player: ServerPlayerEntity) {
        val isOnTeam = BingoTeam.values()
            .any { player.isTeamPlayer(it.team()) }

        val gameMode = when {
            state == GameState.PREGAME -> GameMode.ADVENTURE
            state == GameState.PLAYING && isOnTeam -> GameMode.SURVIVAL
            else -> GameMode.SPECTATOR
        }

        player.changeGameMode(gameMode)
    }

    fun giveMapCardItem(player: ServerPlayerEntity) {
        val hasMap = player.inventory.containsAny { it.item == Items.FILLED_MAP }
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

        // check win conditions
        updateWinCondition()
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

    fun updateWinCondition() {
        val winner = winCondition.getWinner(card) ?: return
        // TODO: send win message/etc

        minecraftServer.playerManager.playerList.forEach { player ->
            minecraftServer.playerManager.broadcast(
                Text.literal("${winner.name} team is the winner!")
                    .formatted(Formatting.BOLD)
                    .formatted(winner.formatting),
                false,
            )
        }

        end()
    }

    fun updateTimer() {
        if (state != GameState.PLAYING) return
        val start = startedAt ?: return

        val duration = Duration.between(start, Instant.now())

        minecraftServer.playerManager.playerList.forEach { player ->
            timeBar.addPlayer(player)
        }

        val secondsRemaining = Duration.ofSeconds(timeLimit.seconds - duration.toSeconds())
        val secondsRemainingStr = String.format(Locale.US, "%02dh %02dm %02ds", secondsRemaining.toHoursPart(), secondsRemaining.toMinutesPart(), secondsRemaining.toSecondsPart())

        timeBar.name = Text.literal("Time Remaining: $secondsRemainingStr")
        timeBar.value = ((1f - (duration.toSeconds() / timeLimit.seconds.toFloat())) * timeBar.maxValue).toInt()

        if (duration.toMinutes() > timeLimit.minutes)
            end()
    }

}