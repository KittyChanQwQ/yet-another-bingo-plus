package me.jfenn.bingo.common

import net.minecraft.server.MinecraftServer
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

const val MOD_ID = "bingo"

val LOBBY_WORLD_ID = Identifier("bingo", "lobby")
val MinecraftServer.lobbyWorld get() = worlds.find { it.registryKey.value == LOBBY_WORLD_ID }

// if true, item should be ignored when checking for bingo items
const val NBT_BINGO_IGNORE = "bingo_ignore"
// if true, item should not drop as an entity
const val NBT_BINGO_VANISH = "bingo_vanish"

val BINGO_WORLD_PREFIX = "${Formatting.GREEN}[BINGO]${Formatting.WHITE}"

const val BINGO_VERSION = "1.1.12"

val PACKET_CARD_DISPLAY = Identifier("bingo", "display_cards")
val PACKET_CARD_UPDATE = Identifier("bingo", "update_card")
val PACKET_CARD_OPEN = Identifier("bingo", "open_card")
