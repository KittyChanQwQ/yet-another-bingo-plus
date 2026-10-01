package me.jfenn.bingo.platform

import me.jfenn.bingo.platform.block.BlockPosition
import java.util.*

sealed interface HitResult

data class BlockHitResult(
    val blockPos: BlockPosition,
) : HitResult

data class EntityHitResult(
    val entityId: UUID,
) : HitResult
