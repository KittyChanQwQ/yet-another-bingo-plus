package me.jfenn.bingo.api

import me.jfenn.bingo.config.BlockPosition
import net.minecraft.server.world.ServerWorld
import java.io.InputStream

interface IStructureManager {

    fun placeStructure(world: ServerWorld, blockPosition: BlockPosition, nbtStream: InputStream)

}