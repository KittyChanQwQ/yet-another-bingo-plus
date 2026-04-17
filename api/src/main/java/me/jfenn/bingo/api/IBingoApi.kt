package me.jfenn.bingo.api

import me.jfenn.bingo.api.card.ICardService
import me.jfenn.bingo.api.config.IBingoConfig
import me.jfenn.bingo.api.data.IBingoGame
import me.jfenn.bingo.api.data.IBingoPlayerStats
import me.jfenn.bingo.api.data.IBingoTeams
import me.jfenn.bingo.api.options.IOptionsService
import net.minecraft.server.MinecraftServer
import java.util.*

interface IBingoApi {
    val server: MinecraftServer
    val game: IBingoGame
    val teams: IBingoTeams
    fun getPlayerStats(id: UUID): IBingoPlayerStats
    val config: IBingoConfig
    val cards: ICardService
    val options: IOptionsService
}