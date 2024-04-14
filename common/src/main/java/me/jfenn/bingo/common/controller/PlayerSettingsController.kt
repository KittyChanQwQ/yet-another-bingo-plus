package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.KEYBIND_OPEN_CARD
import me.jfenn.bingo.common.NBT_BINGO_IGNORE
import me.jfenn.bingo.common.NBT_BINGO_VANISH
import me.jfenn.bingo.common.Permission
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.config.PlayerSettingsService
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.event.packet.ServerPacketEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.generated.BINGO_VERSION
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.integrations.PermissionsIntegration
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger

class PlayerSettingsController(
    events: ScopedEvents,
    private val log: Logger,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val permissions: PermissionsIntegration,
    private val cardViewService: CardViewService,
    private val textSerializer: ITextSerializer,
    private val scoreboardManager: IScoreboardManager,
    private val playerSettingsService: PlayerSettingsService,
    private val text: TextProvider,
    private val packetManager: ServerPacketEvents,
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
            .let { textSerializer.toJson(it) }

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
            .let { textSerializer.toJson(it) }

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
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set bossbar ${!settings.bossbar}"))
                    }
            )
            .append("\n\n")
            .append(
                toggleText(settings.scoreboard, StringKey.PlayerSettingsScoreboard, StringKey.PlayerSettingsScoreboardDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreboard ${!settings.scoreboard}"))
                    }
            )
            .also {
                if (!cardViewService.supportsCardHud(player)) {
                    it.append("\n")
                    it.append(
                        toggleText(settings.scoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHide, StringKey.PlayerSettingsScoreboardAutoHideDescription)
                            .also {
                                it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreboardAutoHide ${!settings.scoreboardAutoHide}"))
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
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set leadingMessages ${!settings.leadingMessages}"))
                    }
            )
            .append("\n")
            .append(
                toggleText(settings.scoreMessages, StringKey.PlayerSettingsMessagesLines, StringKey.PlayerSettingsMessagesLinesDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreMessages ${!settings.scoreMessages}"))
                    }
            )
            .append("\n\n")
            .append(
                toggleText(settings.nightVision, StringKey.PlayerSettingsNightVision, StringKey.PlayerSettingsNightVisionDescription)
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set nightVision ${!settings.nightVision}"))
                    }
            )
            .let { textSerializer.toJson(it) }

    private fun isBookItem(stack: ItemStack) = stack.item == Items.WRITTEN_BOOK
            && stack.nbt?.getBoolean(NBT_BINGO_IGNORE) == true
            && stack.nbt?.getBoolean(NBT_BINGO_VANISH) == true

    private fun createBookItem(player: ServerPlayerEntity) : ItemStack {
        val item = ItemStack(Items.WRITTEN_BOOK, 1)
        item.getOrCreateNbt().apply {
            putBoolean(NBT_BINGO_IGNORE, true)
            putBoolean(NBT_BINGO_VANISH, true)
            putString("title", text.raw(StringKey.FullName))
            putString("author", scoreboardManager.getPlayerName(player))
        }
        updateBookItem(player, item)
        return item
    }

    private fun updateBookItem(player: ServerPlayerEntity, stack: ItemStack) {
        val settings = playerSettingsService.getPlayer(player)

        stack.getOrCreateNbt().apply {
            put("pages", NbtList().apply {
                if (settings.seenTutorial) {
                    add(NbtString.of(getSettingsPage(player, settings)))
                    add(NbtString.of(getIntroPage(player)))
                    add(NbtString.of(getHintsPage(player)))
                } else {
                    add(NbtString.of(getIntroPage(player)))
                    add(NbtString.of(getSettingsPage(player, settings)))
                    add(NbtString.of(getHintsPage(player)))
                }
            })
        }
    }

    init {
        events.onGameTick {
            if (state.state != GameState.PREGAME)
                return@onGameTick

            for (player in server.playerManager.playerList) {
                var bookCount = 0
                for (stack in player.allHeldStacks()) {
                    if (isBookItem(stack)) {
                        // prevent players from taking someone else's book...
                        if (stack.nbt?.getString("author") != scoreboardManager.getPlayerName(player))
                            updateBookItem(player, stack)

                        bookCount++

                        if (bookCount > 1)
                            stack.count = 0
                        else if (stack.count > 1)
                            stack.count = 1
                    }
                }

                if (bookCount == 0) {
                    val bookItem = createBookItem(player)
                    player.giveItemStack(bookItem)
                }
            }
        }

        events.onChangePlayerSettings { player ->
            if (state.state == GameState.PREGAME) {
                player.allHeldStacks()
                    .filter { isBookItem(it) }
                    .forEach { updateBookItem(player, it) }
            }

            val settings = playerSettingsService.getPlayer(player)
            packetManager.receivePlayerSettingsV1.send(player, settings)
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
                player.allHeldStacks()
                    .filter { isBookItem(it) }
                    .forEach { updateBookItem(player, it) }
            }
        }
    }

}