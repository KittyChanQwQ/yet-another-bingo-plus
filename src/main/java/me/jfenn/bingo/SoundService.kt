package me.jfenn.bingo

import me.jfenn.bingo.data.BingoTeam
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

object SoundService {
    fun playTileCollected(team: BingoTeam) {
        for (player in minecraftServer.playerManager.playerList) {
            if (player.isTeamPlayer(team.team())) {
                player.networkHandler.sendPacket(PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_PLAYER_LEVELUP),
                    SoundCategory.BLOCKS,
                    player.x, player.y, player.z,
                    1f, 2f, 0
                ))
            } else {
                player.networkHandler.sendPacket(PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value()),
                    SoundCategory.BLOCKS,
                    player.x, player.y, player.z,
                    1f, 1f, 0
                ))
            }
        }
    }
}