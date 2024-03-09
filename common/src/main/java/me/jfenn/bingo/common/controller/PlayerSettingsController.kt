package me.jfenn.bingo.common.controller

import me.jfenn.bingo.api.IScoreboardManager
import me.jfenn.bingo.api.ITextSerializer
import me.jfenn.bingo.common.*
import me.jfenn.bingo.common.config.ConfigService
import me.jfenn.bingo.common.config.PlayerSettings
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.map.CardViewService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.allHeldStacks
import me.jfenn.bingo.integrations.PermissionsIntegration
import me.jfenn.bingo.integrations.VanishIntegration
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

class PlayerSettingsController(
    events: ScopedEvents,
    private val server: MinecraftServer,
    private val state: BingoState,
    private val permissions: PermissionsIntegration,
    private val cardViewService: CardViewService,
    private val textSerializer: ITextSerializer,
    private val scoreboardManager: IScoreboardManager,
    private val configService: ConfigService,
) : BingoComponent() {

    private fun getIntroPage(player: ServerPlayerEntity) =
        Text.empty()
            .append(
                Text.literal("Welcome to ")
                    .append(Text.literal("Yet Another MC BINGO").formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.horrific.dev/bingo/"))
                    })
                    .append("!")
                    .formatted(Formatting.BOLD)
            )
            .append("\n")
            .append(Text.literal("v$BINGO_VERSION").formatted(Formatting.GRAY))
            .also {
                if (permissions.hasPermission(player, Permission.CONFIGURE_GAME)) {
                    it.append("\n\nUse the menu in front of you to configure the game.")
                }
            }
            .append("\n\n")
            .append(
                Text.literal("Read ")
                    .append(Text.literal("how to play").formatted(Formatting.DARK_GREEN, Formatting.UNDERLINE).also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://minecraft.horrific.dev/bingo/what-is-bingo"))
                    })
                    .append(Text.literal(" on the wiki."))
            )
            .also {
                if (!permissions.hasPermission(player, Permission.CONFIGURE_GAME)) {
                    it.append("\n\nChange your player settings on the next page.")
                }
            }
            .append("\n\nHave fun!")
            .let { textSerializer.toJson(it) }

    private fun getHintsPage(player: ServerPlayerEntity) =
        Text.empty()
            .append(Text.literal("Hints").formatted(Formatting.BOLD))
            .append("\n\n")
            .append(when {
                cardViewService.supportsCardHud(player) -> Text.empty()
                    .append("Press [")
                    .append(Text.keybind(KEYBIND_OPEN_CARD).formatted(Formatting.DARK_GREEN))
                    .append("] to see the Bingo Card in an inventory view.")
                else -> Text.empty()
                    .append("When holding a Bingo Card, press [")
                    .append(Text.keybind("key.use").formatted(Formatting.DARK_GREEN))
                    .append("] to see its items.")
            })
            .also {
                if (permissions.hasPermission(player, Permission.COMMAND_COORDS)) {
                    it.append("\n\nUse \"/coords\" to keep track of coordinates and share them with your team.")
                }
            }
            .let { textSerializer.toJson(it) }

    private fun toggleText(
        value: Boolean,
        title: String,
        description: String? = null,
    ) =
        Text.empty()
            .append(
                Text.literal(if (value) "☑" else "☐")
                    .formatted(
                        if (value) Formatting.DARK_GREEN else Formatting.DARK_RED,
                    )
            )
            .append(" $title")
            .also { text ->
                if (description != null) {
                    text.append(Text.literal(" \uD83D\uDEC8").formatted(Formatting.GRAY).also {
                        it.style = it.style.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(description)))
                    })
                }
            }

    private fun getSettingsPage(settings: PlayerSettings) =
        Text.empty()
            .append(Text.literal("Player Settings").formatted(Formatting.BOLD))
            .append("\n\n")
            .append(
                toggleText(settings.bossbar, "Bossbar", "Shows the time remaining in a bossbar.")
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set bossbar ${!settings.bossbar}"))
                    }
            )
            .append("\n\n")
            .append(
                toggleText(settings.scoreboard, "Scoreboard", "Shows game info and time remaining in a scoreboard.")
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreboard ${!settings.scoreboard}"))
                    }
            )
            .append("\n")
            .append(
                toggleText(settings.scoreboardAutoHide, "Auto Hide", "Hides the scoreboard when you aren't holding the bingo card.")
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreboardAutoHide ${!settings.scoreboardAutoHide}"))
                    }
            )
            .append("\n\nMessages:\n")
            .append(
                toggleText(settings.leadingMessages, "Leading Team", "Send chat messages when the leading team is changed.")
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set leadingMessages ${!settings.leadingMessages}"))
                    }
            )
            .append("\n")
            .append(
                toggleText(settings.scoreMessages, "Lines Scored", "Send chat messages when a line is scored.")
                    .also {
                        it.style = it.style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bingo set scoreMessages ${!settings.scoreMessages}"))
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
            putString("title", "Yet Another Minecraft BINGO")
            putString("author", scoreboardManager.getPlayerName(player))
        }
        updateBookItem(player, item)
        return item
    }

    private fun updateBookItem(player: ServerPlayerEntity, stack: ItemStack) {
        val settings = configService.getPlayer(player.uuidAsString)

        stack.getOrCreateNbt().apply {
            put("pages", NbtList().apply {
                if (settings.seenTutorial) {
                    add(NbtString.of(getSettingsPage(settings)))
                    add(NbtString.of(getIntroPage(player)))
                    add(NbtString.of(getHintsPage(player)))
                } else {
                    add(NbtString.of(getIntroPage(player)))
                    add(NbtString.of(getSettingsPage(settings)))
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
            player.allHeldStacks()
                .filter { isBookItem(it) }
                .forEach { updateBookItem(player, it) }
        }

        events.onUpdateTick {
            if (state.state != GameState.PREGAME)
                return@onUpdateTick

            for (player in server.playerManager.playerList) {
                player.allHeldStacks()
                    .filter { isBookItem(it) }
                    .forEach { updateBookItem(player, it) }
            }
        }
    }

}