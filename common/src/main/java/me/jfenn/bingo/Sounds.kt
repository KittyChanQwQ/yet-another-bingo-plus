package me.jfenn.bingo

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.state.BingoContext
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

    fun playTileCollected(ctx: BingoContext, team: BingoTeam) {
        val teamInstance = team.team(ctx.server)
        for (player in ctx.server.playerManager.playerList) {
            if (player.isTeamPlayer(teamInstance)) {
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