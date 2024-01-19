package me.jfenn.bingo.common.event.model

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.world.World

data class UseItemEvent(
    val player: PlayerEntity,
    val world: World,
    val hand: Hand,
)
