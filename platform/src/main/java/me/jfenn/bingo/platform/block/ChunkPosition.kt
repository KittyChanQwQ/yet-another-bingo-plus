package me.jfenn.bingo.platform.block

data class ChunkPosition(val x: Int, val z: Int) {
    fun toBlockPos() = BlockPosition(x * 16, 0, z * 16)
    fun getBlockPos(x: Int, y: Int, z: Int) = toBlockPos().move(x, y, z)
}