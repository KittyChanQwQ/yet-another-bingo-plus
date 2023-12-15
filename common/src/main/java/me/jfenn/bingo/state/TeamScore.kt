package me.jfenn.bingo.state

import kotlinx.serialization.Serializable

@Serializable
data class TeamScore(
    val items: Int,
    val lines: Int,
) {

    companion object {
        val ZERO = TeamScore(0, 0)
    }

}
