package me.jfenn.bingo.common

import me.jfenn.bingo.generated.StringKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

const val MOD_ID = "bingo"

const val MOD_ID_BINGO = "yet-another-minecraft-bingo"

val LOBBY_WORLD_ID = Identifier.of("bingo", "lobby")!!
val MinecraftServer.lobbyWorld get() = worlds.find { it.registryKey.value == LOBBY_WORLD_ID }

// if true, item should be ignored when checking for bingo items
const val NBT_BINGO_IGNORE = "bingo_ignore"
// if true, item should not drop as an entity
const val NBT_BINGO_VANISH = "bingo_vanish"
// if true, player should normally be unable to remove the item from their inventory
const val NBT_BINGO_KEEP = "bingo_keep"

// indicates the bingo tutorial book item
const val NBT_BINGO_INFO_BOOK = "bingo_info_book"

const val NBT_BINGO_CARD = "bingo_card"
// prefixes bingo card items with the card UUID
const val NBT_BINGO_CARD_PREFIX = "bingo_card_"

const val URL_WIKI = "https://minecraft.horrific.dev/bingo/"
const val URL_WIKI_WHAT_IS_BINGO = "https://minecraft.horrific.dev/bingo/what-is-bingo"

val BINGO_WORLD_PREFIX = "${Formatting.GREEN}[BINGO]${Formatting.WHITE}"

val KEYBIND_OPEN_CARD = StringKey.KeybindOpenCard.key
val KEYBIND_TOGGLE_HUD = StringKey.KeybindToggleHud.key
