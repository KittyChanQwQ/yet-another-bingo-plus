package me.jfenn.bingo.common.chat

import me.jfenn.bingo.common.config.BingoConfig
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.platform.commands.ICommandManager
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.model.AllowChatMessageEvent
import me.jfenn.bingo.platform.scope.BingoKoin

class ChatCommand(
    commandManager: ICommandManager,
    eventBus: IEventBus,
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
        eventBus.register(AllowChatMessageEvent) { (message, player) ->
            if (bypassAllowMessageCheck) return@register true
            val scope = BingoKoin.getScope(player.player.server) ?: return@register true
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