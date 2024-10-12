package me.jfenn.bingo.api.block

import net.minecraft.block.Block
import org.joml.Vector3i

interface IBlockState {
    val identifier: String
    val block: Block
}

interface IWallSignBlockState: IBlockState {
    val facing: Vector3i
}
