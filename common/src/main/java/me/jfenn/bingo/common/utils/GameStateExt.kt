package me.jfenn.bingo.common.utils

import me.jfenn.bingo.api.state.GameState

val GameState.name get() = identifier.replace(Regex("[^a-zA-Z0-9_]"), "_")
