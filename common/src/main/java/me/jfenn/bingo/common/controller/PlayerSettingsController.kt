package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.api.item.IWrittenBook
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.commands.BingoPrefsCommand
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.BINGO_VERSION
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class PlayerSettingsController(
    events: ScopedEvents,
    private val log: Logger,
    private val state: BingoState,
    private val permissions: PermissionsIntegration,
    private val cardViewService: CardViewService,
    private val scoreboardManager: IScoreboardManager,
    private val playerSettingsService: PlayerSettingsService,
    private val text: TextProvider,
    private val packetManager: ServerPacketEvents,
    private val itemStackFactory: IItemStackFactory,
    private val playerManager: IPlayerManager,
) : BingoComponent() {

    private fun getIntroPage(player: ServerPlayerEntity) =
        Text.empty()
            .append(
                text.string(
                    StringKey.IntroWelcome,
                    text.string(StringKey.IntroWelcomeLink).formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.horrific.dev/bingo/"))
                    },
                ).formatted(Formatting.BOLD)
            )
            .append("\n")
            .append(Text.literal("v$BINGO_VERSION").formatted(Formatting.GRAY))
            .also {
                if (permissions.hasPermission(player, Permission.CONFIGURE_GAME)) {
                    it.append("\n\n").append(text.string(StringKey.IntroUseConfigMenu))
                }
            }
            .append("\n\n")
            .append(
                text.string(
                    StringKey.IntroReadHowToPlay,
                    text.string(StringKey.IntroReadHowToPlayLink).formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.horrific.dev/bingo/what-is-bingo"))
                    },
                )
            )
            .also {
                if (!permissions.hasPermission(player, Permission.CONFIGURE_GAME)) {
                    it.append("\n\n").append(text.string(StringKey.IntroChangePlayerSettings))
                }
            }
            .append("\n\n")
            .append(text.string(StringKey.IntroHaveFun))

    private fun getHintsPage(player: ServerPlayerEntity) =
        Text.empty()
            .append(Text.literal("Hints").formatted(Formatting.BOLD))
            .append("\n\n")
            .append(when {
                cardViewService.supportsCardHud(player) -> text.string(
                    StringKey.IntroKeybindHud,
                    Text.keybind(KEYBIND_OPEN_CARD).formatted(Formatting.DARK_GREEN),
                )
                else -> text.string(
                    StringKey.IntroKeybindMap,
                    Text.keybind("key.use").formatted(Formatting.DARK_GREEN),
                )
            })
            .also {
                if (permissions.hasPermission(player, Permission.COMMAND_COORDS)) {
                    it.append("\n\n").append(text.string(StringKey.IntroUseCoords))
                }
            }

    private fun toggleText(
        value: Boolean,
        name: StringKey,
        description: StringKey,
    ) =
        Text.empty()
            .append(
                Text.literal(if (value) "☑" else "☐")
                    .formatted(
                        if (value) Formatting.DARK_GREEN else Formatting.DARK_RED,
                    )
            )
            .append(" ")
            .append(text.string(name))
            .append(
                Text.literal(" \uD83D\uDEC8").formatted(Formatting.GRAY).also {
                    it.style = it.style.withHoverEvent(HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        text.string(description)
                    ))
                }
            )

    private fun getSettingsPage(player: ServerPlayerEntity, settings: PlayerSettings) =
        Text.empty()
            .append(text.string(StringKey.PlayerSettings).formatted(Formatting.BOLD))
            .append("\n\n")
            .append(
                toggleText(settings.bossbar, StringKey.PlayerSettingsBossbar, StringKey.PlayerSettingsBossbarDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::bossbar, !settings.bossbar)))
                    }
            )
            .append("\n\n")
            .append(
                toggleText(settings.scoreboard, StringKey.PlayerSettingsScoreboard, StringKey.PlayerSettingsScoreboardDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::scoreboard, !settings.scoreboard)))
                    }
            )
            .also {
                if (!cardViewService.supportsCardHud(player)) {
                    it.append("\n")
                    it.append(
                        toggleText(settings.scoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHideDescription)
                            .also {
                                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::scoreboardAutoHide, !settings.scoreboardAutoHide)))
                            }
                    )
                }
            }
            .append("\n\n")
            .append(text.string(StringKey.PlayerSettingsMessages))
            .append(":\n")
            .append(
                toggleText(settings.leadingMessages, StringKey.PlayerSettingsMessagesLeading, StringKey.PlayerSettingsMessagesLeadingDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::leadingMessages, !settings.leadingMessages)))
                    }
            )
            .append("\n")
            .append(
                toggleText(settings.scoreMessages, StringKey.PlayerSettingsMessagesLines, StringKey.PlayerSettingsMessagesLinesDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::scoreMessages, !settings.scoreMessages)))
                    }
            )
            .append("\n\n")
            .append(
                toggleText(settings.nightVision, StringKey.PlayerSettingsNightVision, StringKey.PlayerSettingsNightVisionDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, BingoPrefsCommand.getCommand(PlayerSettings::nightVision, !settings.nightVision)))
                    }
            )

    private fun isBookItem(stack: IWrittenBook) =
            stack.hasCustomTag(NBT_BINGO_IGNORE) && stack.hasCustomTag(NBT_BINGO_VANISH)

    private fun createBookItem(player: ServerPlayerEntity) : IWrittenBook {
        val stack = itemStackFactory.createWrittenBook()
        stack.addCustomTag(NBT_BINGO_IGNORE)
        stack.addCustomTag(NBT_BINGO_VANISH)
        stack.title =  text.raw(StringKey.FullName)
        stack.author = scoreboardManager.getPlayerName(player)
        updateBookItem(player, stack)
        return stack
    }

    private fun updateBookItem(player: ServerPlayerEntity, stack: IWrittenBook) {
        val settings = playerSettingsService.getPlayer(player)

        stack.setPages(buildList {
            if (settings.seenTutorial) {
                add(getSettingsPage(player, settings))
                add(getIntroPage(player))
                add(getHintsPage(player))
            } else {
                add(getIntroPage(player))
                add(getSettingsPage(player, settings))
                add(getHintsPage(player))
            }
        })
    }

    init {
        events.onGameTick {
            if (state.state != GameState.PREGAME)
                return@onGameTick

            for (player in playerManager.getPlayers()) {
                var bookCount = 0
                for (stack in player.allHeldStacks().mapNotNull { it.asWrittenBook() }) {
                    if (isBookItem(stack)) {
                        // prevent players from taking someone else's book...
                        if (stack.author != scoreboardManager.getPlayerName(player.player))
                            updateBookItem(player.player, stack)

                        bookCount++

                        if (bookCount > 1)
                            stack.count = 0
                        else if (stack.count > 1)
                            stack.count = 1
                    }
                }

                if (bookCount == 0) {
                    val bookItem = createBookItem(player.player)
                    player.giveItemStack(bookItem)
                }
            }
        }

        events.onChangePlayerSettings { player ->
            if (state.state == GameState.PREGAME) {

                playerManager.forPlayer(player).allHeldStacks()
                    .mapNotNull { it.asWrittenBook() }
                    .filter { isBookItem(it) }
                    .forEach { updateBookItem(player, it) }
            }

            val settings = playerSettingsService.getPlayer(player)
            when {
                packetManager.receivePlayerSettingsV2.send(player, settings) -> {}
                packetManager.receivePlayerSettingsV1.send(player, settings) -> {}
            }
        }

        events.onPlayerSettingsPacket {
            if (permissions.hasPermission(it.player, Permission.CONFIGURE_PLAYER)) {
                log.debug("[PlayerSettingsController] Server - processing settings change from {}", it.player)
                playerSettingsService.setPlayer(it.player, it.settings)
            } else {
                log.debug(
                    "[PlayerSettingsController] Server - denying settings change from {} due to permissions",
                    it.player
                )
            }
        }

        events.onPlayerChannelRegister { player ->
            if (state.state == GameState.PREGAME) {
                playerManager.forPlayer(player)
                    .allHeldStacks()
                    .mapNotNull { it.asWrittenBook() }
                    .filter { isBookItem(it) }
                    .forEach { updateBookItem(player, it) }
            }
        }
    }

}