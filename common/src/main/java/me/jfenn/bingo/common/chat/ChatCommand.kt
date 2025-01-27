package me.jfenn.bingo.common.chat

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.platform.scope.BingoKoin
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.IServerEvents
import me.jfenn.bingo.platform.commands.ICommandManager

class ChatCommand(
    commandManager: ICommandManager,
    serverEvents: IServerEvents,
    private val config: BingoConfig,
) {
    companion object {
        var bypassAllowMessageCheck = false
    }

    init {
        // send a message to all teams, outside of the team-local chat
        for (cmd in config.chat.globalCommandAliases) {
            commandManager.register(cmd) {
                signedMessage("message") { messageArg ->
                    executes {
                        val sender = playerOrThrow
                        getArgument(messageArg).thenAccept {
                            scope.get<ChatMessageService>().sendGlobalMessage(it, sender)
                        }
                    }
                }
            }
        }

        // send a message to the team-local chat
        for (cmd in config.chat.teamCommandAliases) {
            commandManager.register(cmd) {
                signedMessage("message") { messageArg ->
                    executes {
                        val sender = playerOrThrow
                        getArgument(messageArg).thenAccept {
                            scope.get<ChatMessageService>().sendTeamMessage(it, sender)
                        }
                    }
                }
            }
        }

        // override default chat to only send team-specific messages
        serverEvents.onAllowChatMessage { (message, player) ->
            if (bypassAllowMessageCheck) return@onAllowChatMessage true
            val scope = BingoKoin.getScope(player.player.server) ?: return@onAllowChatMessage true
            val state = scope.get<BingoState>()

            if (state.state.isPlayingOrCountdown && config.chat.defaultToTeamChat) {
                val sent = scope.get<ChatMessageService>()
                    .sendTeamMessage(message, player)
                !sent
            } else {
                true
            }
        }
    }
}