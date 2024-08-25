package me.jfenn.bingo.api

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
