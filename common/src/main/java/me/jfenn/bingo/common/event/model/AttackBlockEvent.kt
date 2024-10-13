package me.jfenn.bingo.common.event.model

import me.jfenn.bingo.api.IPlayerHandle
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class AttackBlockEvent(
    val server: MinecraftServer,
    val player: IPlayerHandle,
    val world: World,
    val hand: Hand,
    val blockPos: BlockPos,
    val direction: Direction,
)
