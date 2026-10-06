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

    /**
     * Interact with the game state, round id, time remaining.
     */
    val game: IBingoGame

    /**
     * Get the set of teams in a round.
     */
    val teams: IBingoTeams

    /**
     * Request player statistics from the database.
     */
    fun getPlayerStats(id: UUID): IBingoPlayerStats

    /**
     * Get the mod configuration.
     */
    val config: IBingoConfig

    /**
     * Interact with the card(s) in a round.
     */
    val cards: ICardService

    /**
     * Read or modify the game options in a round.
     */
    val options: IOptionsService
}