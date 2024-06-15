package me.jfenn.bingo.common.commands

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.api.commands.ICommandManager
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class BingoLobbyCommand(
    commandManager: ICommandManager,
    private val text: TextProvider,
) {

    private var readWarnings = mutableMapOf<UUID, Instant>()

    init {
        commandManager.register("bingo") {
            literal("lobby") {
                requires {
                    hasPermission(Permission.CONFIGURE_LOBBY)
                }
                executes {
                    sendMessage(Text.empty())
                    text.string(StringKey.CommandLobbyWarning1)
                        .formatted(Formatting.GOLD)
                        .let { sendMessage(it) }
                    text.string(StringKey.CommandLobbyWarning2)
                        .formatted(Formatting.GOLD)
                        .let { sendMessage(it) }
                    sendMessage(Text.empty())
                    text.string(StringKey.CommandLobbyWarning3)
                        .formatted(Formatting.GOLD)
                        .let { sendMessage(it) }
                    sendMessage(Text.empty())
                    text.string(StringKey.CommandLobbyWarning4)
                        .formatted(Formatting.GRAY)
                        .let { sendMessage(it) }

                    player?.uuid?.let {
                        readWarnings[it] = Clock.System.now()
                    }
                }

                literal("confirm") {
                    executes {
                        val playerId = player?.uuid ?: throw IllegalArgumentException("Must have a player")
                        val readWarning = readWarnings[playerId]

                        if (readWarning != null && readWarning > Clock.System.now() - 5.minutes) {
                            sendMessage(text.string(StringKey.CommandLobbyRestarting))

                            val configService = scope.get<ConfigService>()
                            configService.config.server.isLobbyMode = true
                            configService.writeConfig(configService.config)
                            server.shutdown()
                        } else {
                            sendMessage(text.string(StringKey.CommandLobbyMustRun).formatted(Formatting.RED))
                            throw IllegalArgumentException("Has not read the warnings")
                        }
                    }
                }
            }
        }
    }
}