package me.jfenn.bingo.client.common.sound

import me.jfenn.bingo.common.config.BingoConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.sound.SoundCategory

class SoundService(
    private val clientSounds: ClientSounds,
    private val config: BingoConfig,
) {

    fun play(
        sound: ClientSounds.Key,
        volume: Float = 1f,
        pitch: Float = 1f,
        actualVolume: Float = volume * config.client.getSoundVolume(sound.name.lowercase()),
    ) {
        val instance = PositionedSoundInstance(
            clientSounds[sound].id,
            SoundCategory.RECORDS,
            actualVolume,
            pitch,
            SoundInstance.createRandom(),
            false,
            0,
            SoundInstance.AttenuationType.NONE,
            0.0, 0.0, 0.0, true
        )
        MinecraftClient.getInstance().soundManager.play(instance)
    }

}