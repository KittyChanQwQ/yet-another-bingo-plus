package me.jfenn.bingo.api

import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity

interface IStatusEffectsFactory {
    fun getStatusEffects(entity: LivingEntity): IStatusEffects
}

interface IStatusEffects {

    fun addEffect(type: EffectType, duration: Int, amplifier: Int, ambient: Boolean, visible: Boolean): IStatusEffectHandle

    fun getEffects(): List<IStatusEffectHandle>

    fun removeEffect(effect: IStatusEffectHandle)

}

enum class EffectType {
    NIGHT_VISION,
    SLOWNESS,
    JUMP_BOOST,
    INVISIBILITY,
    OTHER,
}

interface IStatusEffectHandle {
    val type: EffectType
    val duration: Int
}
