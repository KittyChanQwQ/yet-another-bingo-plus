package me.jfenn.bingo.impl

import me.jfenn.bingo.api.EffectType
import me.jfenn.bingo.api.IStatusEffectHandle
import me.jfenn.bingo.api.IStatusEffects
import me.jfenn.bingo.api.IStatusEffectsFactory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.network.ServerPlayerEntity

class StatusEffectsImpl(
    private val player: ServerPlayerEntity,
) : IStatusEffects {
    companion object : IStatusEffectsFactory {
        override fun getStatusEffects(player: ServerPlayerEntity): IStatusEffects {
            return StatusEffectsImpl(player)
        }
    }

    override fun addEffect(
        type: EffectType,
        duration: Int,
        amplifier: Int,
        ambient: Boolean,
        visible: Boolean
    ): IStatusEffectHandle {
        val instance = StatusEffectInstance(
            when (type) {
                EffectType.NIGHT_VISION -> StatusEffects.NIGHT_VISION
                EffectType.SLOWNESS -> StatusEffects.SLOWNESS
                EffectType.JUMP_BOOST -> StatusEffects.JUMP_BOOST
                EffectType.OTHER -> throw IllegalArgumentException("[StatusEffectsImpl] OTHER is not a valid effect type!")
            },
            duration,
            amplifier,
            ambient,
            visible
        )
        player.addStatusEffect(instance)
        return StatusEffectHandle(instance)
    }

    override fun getEffects(): List<IStatusEffectHandle> {
        return player.statusEffects.map { StatusEffectHandle(it) }
    }

    override fun removeEffect(effect: IStatusEffectHandle) {
        require(effect is StatusEffectHandle)
        player.removeStatusEffect(effect.instance.effectType)
    }
}

class StatusEffectHandle(
    val instance: StatusEffectInstance,
) : IStatusEffectHandle {
    override val type: EffectType = when (instance.effectType) {
        StatusEffects.NIGHT_VISION -> EffectType.NIGHT_VISION
        StatusEffects.SLOWNESS -> EffectType.SLOWNESS
        StatusEffects.JUMP_BOOST -> EffectType.JUMP_BOOST
        else -> EffectType.OTHER
    }
    override val duration: Int = instance.duration
}