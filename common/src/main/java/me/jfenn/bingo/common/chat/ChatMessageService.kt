package me.jfenn.bingo.common.chat

import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.platform.IPlayerManager
import me.jfenn.bingo.platform.commands.ISignedMessage
import net.minecraft.text.Text
import net.minecraft.text.Texts

internal class ChatMessageService(
    private val playerManager: IPlayerManager,
    private val teamService: TeamService,
    private val text: TextProvider,
) {
    fun sendGlobalMessage(
        signedMessage: ISignedMessage,
        sender: IPlayerHandle,
    ) {
        try {
            ChatCommand.bypassAllowMessageCheck = true
            playerManager.broadcastChatMessage(signedMessage, sender)
        } finally {
            ChatCommand.bypassAllowMessageCheck = false
        }
    }

    fun sendTeamMessage(
        signedMessage: ISignedMessage,
        sender: IPlayerHandle,
    ): Boolean {
        val team = teamService.getPlayerTeam(sender) ?: return false

        val teamName = Texts.bracketed(team.getSimpleName(text)).formatted(team.textColor)

        playerManager.getPlayers()
            .filter { team.includesPlayer(it) }
            .forEach { it.sendTeamMessage(signedMessage, sender, teamName) }

        return true
    }

    fun sendTeamMessage(
        message: Text,
        sender: IPlayerHandle,
    ): Boolean {
        val team = teamService.getPlayerTeam(sender) ?: return false

        val teamName = Texts.bracketed(team.getSimpleName(text)).formatted(team.textColor)

        playerManager.getPlayers()
            .filter { team.includesPlayer(it) }
            .forEach { it.sendTeamMessage(message, sender, teamName) }

        return true
    }
}