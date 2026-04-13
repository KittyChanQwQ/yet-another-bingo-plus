package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.block.BlockPosition

data class BlockHitResult(
    val blockPos: BlockPosition,
)

data class EntityHitResult(
    val entity: IEntity,
)
