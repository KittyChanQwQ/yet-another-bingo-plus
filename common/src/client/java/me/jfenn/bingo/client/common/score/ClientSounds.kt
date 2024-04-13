package me.jfenn.bingo.client.common.score

import me.jfenn.bingo.common.MOD_ID_BINGO
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier

class ClientSounds {

    companion object {
        val ITEM_SCORED = SoundEvent.of(Identifier(MOD_ID_BINGO, "item_scored"))
        val OPPONENT_SCORED = SoundEvent.of(Identifier(MOD_ID_BINGO, "opponent_scored"))

        val GAME_WON = SoundEvent.of(Identifier(MOD_ID_BINGO, "game_won"))
        val GAME_LOST = SoundEvent.of(Identifier(MOD_ID_BINGO, "game_lost"))

        val TIMER_TICK = SoundEvent.of(Identifier(MOD_ID_BINGO, "timer_tick"))
    }

    init {
        Registry.register(Registries.SOUND_EVENT, ITEM_SCORED.id, ITEM_SCORED)
        Registry.register(Registries.SOUND_EVENT, OPPONENT_SCORED.id, OPPONENT_SCORED)
        Registry.register(Registries.SOUND_EVENT, GAME_WON.id, GAME_WON)
        Registry.register(Registries.SOUND_EVENT, GAME_LOST.id, GAME_LOST)
        Registry.register(Registries.SOUND_EVENT, TIMER_TICK.id, TIMER_TICK)
    }

}