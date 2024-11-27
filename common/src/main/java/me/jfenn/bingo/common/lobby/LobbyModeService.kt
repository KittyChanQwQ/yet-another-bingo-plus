package me.jfenn.bingo.common.lobby

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.jfenn.bingo.platform.IPlayerHandle
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.lobby.BingoLobbyCommand.Companion.CONFIRM_COMMAND
import me.jfenn.bingo.common.lobby.BingoLobbyCommand.Companion.LOBBY_COMMAND
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import java.util.*
import kotlin.time.Duration.Companion.minutes

internal class LobbyModeService(
    private val server: MinecraftServer,
    private val permissions: PermissionsIntegration,
    private val configService: ConfigService,
    private val text: TextProvider,
    private val log: Logger,
) {
    private var readWarnings = mutableMapOf<UUID, Instant>()

    fun canUseLobbyCommand(player: IPlayerHandle) = server.isDedicated && permissions.hasPermission(player, Permission.CONFIGURE_LOBBY)

    fun sendWarnings(player: IPlayerHandle): Sequence<Text> = sequence {
        yield(Text.empty())
        text.string(StringKey.CommandLobbyWarning1)
            .formatted(Formatting.GOLD)
            .let { yield(it) }
        text.string(StringKey.CommandLobbyWarning2)
            .formatted(Formatting.GOLD)
            .let { yield(it) }
        yield(Text.empty())
        text.string(StringKey.CommandLobbyWarning3)
            .formatted(Formatting.GOLD)
            .let { yield(it) }
        yield(Text.empty())
        text.string(StringKey.CommandLobbyWarning4, CONFIRM_COMMAND)
            .formatted(Formatting.GRAY)
            .let { yield(it) }

        readWarnings[player.uuid] = Clock.System.now()
    }

    fun acceptWarnings(player: IPlayerHandle) = sequence {
        val playerId = player.uuid
        val readWarning = readWarnings[playerId]

        if (readWarning != null && readWarning > Clock.System.now() - 5.minutes) {
            yield(text.string(StringKey.CommandLobbyRestarting))

            log.info("[BingoLobbyCommand] ${player.playerName} used '$CONFIRM_COMMAND' and has accepted the warnings.")

            configService.writeConfig(configService.config.apply {
                server.isLobbyMode = true
            })
            server.stop(false)
        } else {
            yield(text.string(StringKey.CommandLobbyMustRun, LOBBY_COMMAND).formatted(
                Formatting.RED
            ))
            throw IllegalArgumentException("Has not read the warnings")
        }
    }
}