package me.jfenn.bingo.client.common.sound

import me.jfenn.bingo.common.MOD_ID_BINGO
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

class ClientSounds {

    // this enum must contain the exact file names in resources/assets/yet-another-minecraft-bingo/sounds/*.ogg
    // as well as a translation key under "bingo.sound.*"
    enum class Key {
        ITEM_SCORED,
        OPPONENT_SCORED,
        GAME_WON,
        GAME_LOST,
        TIMER_TICK,
        START_COUNTDOWN,
        START_RELEASE,
    }

    private val soundEvents = Key.entries.associateWith {
        SoundEvent.of(Identifier.of(MOD_ID_BINGO, it.name.lowercase())!!)
    }

    operator fun get(key: Key) = soundEvents[key]!!

    init {
        for (event in soundEvents.values) {
            Registry.register(Registries.SOUND_EVENT, event.id, event)
        }
    }

}