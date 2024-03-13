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

const val BINGO_VERSION = "1.2.0.beta2"

val PACKET_CARD_DISPLAY_S2C = Identifier("bingo", "display_cards")
val PACKET_CARD_UPDATE_S2C = Identifier("bingo", "update_card")
val PACKET_SETTINGS_UPDATE_S2C = Identifier("bingo", "update_settings_s2c")
val PACKET_SETTINGS_UPDATE_C2S = Identifier("bingo", "update_settings_c2s")
val PACKET_GAME_OVER_S2C = Identifier("bingo", "game_over")

val KEYBIND_OPEN_CARD = "key.$MOD_ID.open_card"
