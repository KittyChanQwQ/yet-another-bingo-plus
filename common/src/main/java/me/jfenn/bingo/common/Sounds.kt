package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d

object Sounds {

    fun playButtonSound(player: ServerPlayerEntity, fromPosition: Vec3d) {
        // This is sent manually to bypass the lobby chaos protection
        val soundEntry = Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_LEVER_CLICK)
        val packet = PlaySoundS2CPacket(
            soundEntry,
            SoundCategory.MASTER,
            fromPosition.x,
            fromPosition.y,
            fromPosition.z,
            0.3f,
            1f,
            player.serverWorld.seed,
        )

        player.networkHandler.sendPacket(packet)
        player.server.playerManager.sendToAround(
            player,
            fromPosition.x,
            fromPosition.y,
            fromPosition.z,
            soundEntry.value().getDistanceToTravel(1f).toDouble(),
            player.world.registryKey,
            packet,
        )
    }

    fun playGameStarted(player: ServerPlayerEntity) {
        player.playSound(
            SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
            SoundCategory.RECORDS,
            1f, 2f
        )
    }

    fun playTileCollected(server: MinecraftServer, team: BingoTeam) {
        for (player in server.playerManager.playerList) {
            if (team.includesPlayer(player)) {
                val score = team.score.items
                player.playSound(
                    SoundEvents.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.RECORDS,
                    1f, 1f + (score / 25f)
                )
            } else {
                player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.RECORDS,
                    1f, 1f
                )
            }
        }
    }

    fun playGameOver(player: ServerPlayerEntity) {
        player.playSound(
            SoundEvents.BLOCK_PORTAL_TRAVEL,
            SoundCategory.RECORDS,
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