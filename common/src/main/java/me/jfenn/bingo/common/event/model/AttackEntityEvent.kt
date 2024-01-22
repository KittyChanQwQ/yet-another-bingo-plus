package me.jfenn.bingo.common.event.model

import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.world.World

data class AttackEntityEvent(
    val player: PlayerEntity,
    val world: World,
    val hand: Hand,
    val entity: Entity,
    val hit: EntityHitResult?,
)