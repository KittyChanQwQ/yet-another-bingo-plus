package me.jfenn.bingo

import me.jfenn.bingo.team.BingoTeam
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents

object Sounds {

    fun playTileCollected(team: BingoTeam) {
        for (player in minecraftServer.playerManager.playerList) {
            if (player.isTeamPlayer(team.team())) {
                player.playSound(
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.BLOCKS,
                    1f, 2f

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