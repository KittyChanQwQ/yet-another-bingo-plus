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

val BINGO_WORLD_PREFIX = "${Formatting.GREEN}[BINGO]${Formatting.WHITE}"

val KEYBIND_OPEN_CARD = StringKey.KeybindOpenCard.key
