package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoOptions
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.spawn.SpawnService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class GameService(
    private val log: Logger,
    private val state: BingoState,
    private val options: BingoOptions,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
) {

    fun start(
        sendFeedback: (String) -> Unit = {},
        allowSpectators: Boolean = false,
    ): Boolean {
        val playersWithoutATeam = server.playerManager.playerList
            .filter { teamService.getPlayerTeam(it) == null }

        if (playersWithoutATeam.isNotEmpty() && !allowSpectators) {
            // If there's more than one player, teams need to be set up manually!
            if (server.playerManager.playerList.size > 1) {
                sendFeedback("Some players have not picked a team! Run '/bingo shuffleteams' or sneak+click to ignore.")
                return false
            }

            // Otherwise, we can just auto-assign a team
            for (player in playersWithoutATeam) {
                teamService.joinTeam(player, BingoTeam.TEAM_BLOCKS.values.random())
            }
        }

        // if the game has already been started, ignore
        if (state.startedAt != null) {
            sendFeedback("The game has already been started! (if this is a bug, try restarting the server...)")
            return false
        }

        if (!options.isValid()) {
            sendFeedback("Item distribution must add up to 25!")
            return false
        }

        // save the current config, if overwrite option is set
        if (ConfigService.config.overwriteOptions) {
            ConfigService.writeOptions(options)
        }

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game is starting!")))
            Sounds.playGameStarted(player)
        }

        // spread player teams to spawnpoint
        spawnService.createSpawnpoints()

        ChatMessages.broadcastStart(server, options)

        // change to the playing state
        state.changeState(events, GameState.PLAYING)

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game has started!")))
            Sounds.playGameStarted(player)
        }

        return true
    }

    fun end(reason: String?) {
        ChatMessages.broadcastEnd(server, options, state, reason)
        state.changeState(events, GameState.POSTGAME)
    }

}