package me.jfenn.bingo.common.autorestart

import kotlinx.datetime.Instant

class RestartTimerState(
    var shutdownAt: Instant? = null,
)
