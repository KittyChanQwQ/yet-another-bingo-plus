package me.jfenn.bingo.common

import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BingoConfig
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

class GameService(
    private val state: BingoState,
    private val config: BingoConfig,
    private val server: MinecraftServer,
    private val events: ScopedEvents,
    private val spawnService: SpawnService,
    private val teamService: TeamService,
) {

    fun start() {
        // if the game has already been started, ignore
        if (state.startedAt != null) return
        if (!config.isValid()) return

        // save the current config, if overwrite option is set
        if (config.overwrite) {
            ConfigService.writeConfig(config)
        }

        for (player in server.playerManager.playerList) {
            val team = teamService.getPlayerTeam(player)
            player.networkHandler.sendPacket(TitleS2CPacket(Text.literal("BINGO").formatted(team?.formatting ?: Formatting.WHITE)))
            player.networkHandler.sendPacket(SubtitleS2CPacket(Text.literal("Game is starting!")))
            Sounds.playGameStarted(player)
        }

        // spread player teams to spawnpoint
        spawnService.createSpawnpoints()

        ChatMessages.broadcastStart(server, config)

        // change to the playing state
        state.changeState(events, GameState.PLAYING)
    }

    fun end(reason: String?) {
        ChatMessages.broadcastEnd(server, config, state, reason)
        state.changeState(events, GameState.POSTGAME)
    }

}