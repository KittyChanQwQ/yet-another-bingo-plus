package me.jfenn.bingo.common.lobby

import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.URL_WIKI_GETTING_STARTED
import me.jfenn.bingo.common.URL_WIKI_SERVER_SETUP
import me.jfenn.bingo.common.commands.BingoPrefsCommand
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameCommands.Companion.START_COMMAND
import me.jfenn.bingo.common.lobby.BingoLobbyCommand.Companion.LOBBY_COMMAND
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.permissions.IPermissionsApi
import me.jfenn.bingo.platform.IPlayerHandle
import net.minecraft.server.MinecraftServer
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting

internal class LobbyModeController(
    events: ScopedEvents,
    private val state: BingoState,
    private val server: MinecraftServer,
    private val lobbyModeService: LobbyModeService,
    private val playerSettingsService: PlayerSettingsService,
    private val permissions: IPermissionsApi,
    private val text: TextProvider,
) {
    private val neverShowAgain get() = Text.literal("  > ")
        .append(
            text.string(StringKey.InstalledNeverShowAgain)
                .formatted(Formatting.GRAY, Formatting.UNDERLINE)
        )
        .apply {
            style = style.withClickEvent(ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                BingoPrefsCommand.getCommand(PlayerSettings::hideLobbyPrompt, true)
            ))
        }

    private fun sendLobbyModeHints(player: IPlayerHandle) {
        player.sendMessage(text.string(StringKey.InstalledSmpMode, LOBBY_COMMAND, START_COMMAND))
        Text.literal("  \uD83D\uDD17 ")
            .append(
                text.string(StringKey.InstalledServerSetupWiki)
                    .formatted(Formatting.GREEN, Formatting.UNDERLINE)
            )
            .apply {
                style = style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, URL_WIKI_SERVER_SETUP))
            }
            .also { player.sendMessage(it) }
        player.sendMessage(neverShowAgain)
    }

    private fun sendLocalSingleplayerHints(player: IPlayerHandle) {
        player.sendMessage(text.string(StringKey.InstalledLimitedSingleplayerMode, START_COMMAND))
        Text.literal("  \uD83D\uDD17 ")
            .append(
                text.string(StringKey.InstalledGettingStartedWiki)
                    .formatted(Formatting.GREEN, Formatting.UNDERLINE)
            )
            .apply {
                style = style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, URL_WIKI_GETTING_STARTED))
            }
            .also { player.sendMessage(it) }
        player.sendMessage(neverShowAgain)
    }

    init {
        events.onPlayerJoin { player ->
            if (state.isLobbyMode) return@onPlayerJoin
            if (playerSettingsService.getPlayer(player).hideLobbyPrompt) return@onPlayerJoin

            if (server.isDedicated && lobbyModeService.canUseLobbyCommand(player))
                sendLobbyModeHints(player)
            if (server.isSingleplayer && permissions.hasPermission(player, Permission.CONFIGURE_GAME))
                sendLocalSingleplayerHints(player)
        }
    }
}