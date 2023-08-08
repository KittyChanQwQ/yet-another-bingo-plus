package me.jfenn.bingo

import me.jfenn.bingo.card.BingoCard
import me.jfenn.bingo.card.BingoCardEntry
import me.jfenn.bingo.card.WinCondition
import me.jfenn.bingo.generator.CardService
import me.jfenn.bingo.map.BingoMapService
import me.jfenn.bingo.spawn.SpreadPlayers
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents

object BingoGame {

    var state = GameState.PREGAME

    var winCondition = WinCondition.THREE_LINES

    var itemDistribution = listOf(2, 6, 9, 6, 2)

    var card: BingoCard = CardService.generate(itemDistribution)

    fun start() {
        card = CardService.generate(itemDistribution)
        state = GameState.PLAYING

        // create the initial card map images
        for (team in BingoTeam.values()) {
            BingoMapService.update(team, card)
        }

        SpreadPlayers.spread()
    }

    fun markTileCollected(tile: BingoCardEntry, team: BingoTeam) {
        tile.setAchieved(team, true)

        // play the doink sound
        playTileCollected(team)

        // update the team map
        BingoMapService.update(team, card)
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

}