package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents

object Sounds {

    fun playGameStarted(player: ServerPlayerEntity) {
        player.playSound(
            SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
            SoundCategory.BLOCKS,
            1f, 2f
        )
    }

    fun playTileCollected(server: MinecraftServer, team: BingoTeam) {
        for (player in server.playerManager.playerList) {
            if (team.includesPlayer(player)) {
                val score = team.score.items
                player.playSound(
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.BLOCKS,
                    1f, 1f + (score / 25f)
                )
            } else {
                player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.BLOCKS,
                    1f, 1f
                )
            }
        }
    }

    fun playGameOver(player: ServerPlayerEntity) {
        player.playSound(
            SoundEvents.BLOCK_PORTAL_TRAVEL,
            SoundCategory.BLOCKS,
            0.2f, 1f
        )
    }

    fun playTeamChanged(player: ServerPlayerEntity) {
        player.playSound(
            SoundEvents.ITEM_LODESTONE_COMPASS_LOCK,
            SoundCategory.BLOCKS,
            1f, 1f
        )
    }

}