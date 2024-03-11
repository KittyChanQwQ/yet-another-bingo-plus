package me.jfenn.bingo.common.event.model

import me.jfenn.bingo.common.config.PlayerSettings
import net.minecraft.server.network.ServerPlayerEntity

class SettingsUpdateEvent(
    val player: ServerPlayerEntity,
    val settings: PlayerSettings,
)