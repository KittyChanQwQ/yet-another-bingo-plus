package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.WinCondition
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameMode

object BingoGame {

    var state = GameState.PREGAME

    var winCondition = WinCondition.THREE_LINES

    var itemDistribution = listOf(2, 6, 9, 6, 2)

    var card: BingoCard = CardService.generate(itemDistribution)

    var teamSpawnpoints = mapOf<BingoTeam, BlockPos>()

    fun start() {
        card = CardService.generate(itemDistribution)
        state = GameState.PLAYING

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
            updateGameMode(player)
            // give each player their team's card
            giveMapCardItem(player)
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
        tile.setAchieved(team, true)

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

        state = GameState.POSTGAME
        minecraftServer.playerManager.playerList.forEach { player ->
            updateGameMode(player)
        }
    }

}