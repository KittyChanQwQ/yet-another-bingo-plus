package me.jfenn.bingo.common

import net.minecraft.util.Formatting

// if true, item should be ignored when checking for bingo items
const val NBT_BINGO_IGNORE = "bingo_ignore"
// if true, item should not drop as an entity
const val NBT_BINGO_VANISH = "bingo_vanish"

val BINGO_WORLD_PREFIX = "${Formatting.GREEN}[BINGO]${Formatting.WHITE}"
