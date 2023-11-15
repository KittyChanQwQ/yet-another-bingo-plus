package me.jfenn.bingo.config

import kotlinx.serialization.Serializable
import net.minecraft.util.math.BlockPos

@Serializable
data class BlockPosition(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    companion object {

        fun fromBlockPos(pos: BlockPos) = BlockPosition(pos.x, pos.y, pos.z)

    }

    fun toBlockPos() = BlockPos(x, y, z)

    override fun toString(): String {
        return "[$x, $y, $z]"
    }

}