package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IStructureManager
import me.jfenn.bingo.common.config.BlockPosition
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtTagSizeTracker
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import java.io.InputStream

class StructureManager : IStructureManager {

    override fun placeStructure(world: ServerWorld, blockPosition: BlockPosition, nbtStream: InputStream) {
        val nbt = nbtStream.use { NbtIo.readCompressed(it, NbtTagSizeTracker(Long.MAX_VALUE, 512)) }

        val template = world.structureTemplateManager.createTemplate(nbt)
        val placement = StructurePlacementData()

        val posOffset = template.size.run { blockPosition.toBlockPos().add(-x/2, -y/2, -z/2) }

        template.place(
            world,
            posOffset,
            posOffset,
            placement,
            StructureBlockBlockEntity.createRandom(world.seed),
            2
        )
    }

}