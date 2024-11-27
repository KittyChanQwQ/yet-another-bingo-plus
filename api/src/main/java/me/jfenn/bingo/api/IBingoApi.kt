package me.jfenn.bingo.api

import me.jfenn.bingo.api.data.BingoGame
import me.jfenn.bingo.api.data.IBingoPlayerStats
import me.jfenn.bingo.api.data.IBingoTeams
import java.util.*

interface IBingoApi {
    val game: BingoGame
    val teams: IBingoTeams
    fun getPlayerStats(id: UUID): IBingoPlayerStats
}