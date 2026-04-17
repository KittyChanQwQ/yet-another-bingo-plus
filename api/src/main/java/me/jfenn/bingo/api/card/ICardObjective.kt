package me.jfenn.bingo.api.card

import java.util.*

interface ICardObjective {
    fun hasPlayerAchieved(playerId: UUID): Boolean
    fun hasTeamAchieved(teamName: String): Boolean
}