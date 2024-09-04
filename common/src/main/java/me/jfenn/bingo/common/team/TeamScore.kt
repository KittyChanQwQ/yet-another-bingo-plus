package me.jfenn.bingo.common.team

import kotlinx.serialization.Serializable

@Serializable
data class TeamScore(
    val items: Int,
    val lines: Int,
    val cards: Int,
) {

    companion object {
        val ZERO = TeamScore(0, 0, 0)
    }

}