package me.jfenn.bingo.api.options

import kotlinx.serialization.Serializable

@Serializable
enum class StalemateBehavior {
    END_GAME,
    REROLL_CARD,
    NOTHING;
}
