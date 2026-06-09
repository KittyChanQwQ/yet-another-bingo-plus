package me.jfenn.bingo.client.impl

import me.jfenn.bingo.client.platform.IClientSoundHandle
import me.jfenn.bingo.client.platform.IClientSoundManager
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource

class ClientSoundManager : IClientSoundManager {

    override fun createUnregistered(id: String): IClientSoundHandle {
        return ClientSoundHandle(Identifier.parse(id))
    }

    override fun play(sound: IClientSoundHandle, volume: Float, pitch: Float) {
        require(sound is ClientSoundHandle)

        val instance = SimpleSoundInstance(
            sound.id,
            SoundSource.UI,
            volume,
            pitch,
            SoundInstance.createUnseededRandom(),
            false,
            0,
            SoundInstance.Attenuation.NONE,
            0.0, 0.0, 0.0, true
        )
        Minecraft.getInstance().soundManager.stop(sound.id, null)
        Minecraft.getInstance().soundManager.play(instance)
    }

    class ClientSoundHandle(
        val id: Identifier,
    ) : IClientSoundHandle
}