package me.jfenn.bingo.common.commands

import me.jfenn.bingo.common.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.commands.IExecutionContext
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class ChatCommand(
    commandManager: ICommandManager,
) {

    companion object {
        fun sendTeamMessage(
            player: IPlayerHandle,
            team: BingoTeam,
            message: Text,
            playerManager: IPlayerManager,
        ) {
            val messageText = Text.empty()
                .append(Text.empty().append("TEAM ").append(player.playerName).formatted(team.formatting))
                .append(Text.literal(": "))
                .append(message)

            // broadcast message to team
            playerManager.getPlayers()
                .filter { team.includesPlayer(it) }
                .forEach { it.sendMessage(messageText) }
        }
    }

    private fun IExecutionContext.sendGlobalMessage(messageStr: String) {
        val player = playerOrThrow
        val teamService = scope.get<TeamService>()
        val playerManager = scope.get<IPlayerManager>()

        val teamColor = teamService.getPlayerTeam(player)
            ?.formatting
            ?: Formatting.RESET

        val message = Text.empty()
            .append(Text.literal("<"))
            .append(
                Text.literal(player.playerName)
                    .formatted(teamColor)
            )
            .append(Text.literal("> $messageStr"))

        playerManager.getPlayers()
            .forEach { it.sendMessage(message) }
    }

    init {
        // send a message to all teams, outside of the team-local chat
        for (cmd in arrayOf("all", "g", "global")) {
            commandManager.register(cmd) {
                string("message", greedy = true) { messageArg ->
                    executes { sendGlobalMessage(getArgument(messageArg)) }
                }
            }
        }

        // override default chat to only send team-specific messages
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, player, _ ->
            val scope = BingoKoin.getScope(player.server) ?: return@register true
            val state = scope.get<BingoState>()
            val teamService = scope.get<TeamService>()
            val playerManager = scope.get<IPlayerManager>()

            if (state.state != GameState.PLAYING) return@register true
            val team = teamService.getPlayerTeam(player) ?: return@register true

            sendTeamMessage(playerManager.forPlayer(player), team, message.content, playerManager)

            false
        }
    }
}