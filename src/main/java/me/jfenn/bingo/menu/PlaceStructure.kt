package me.jfenn.bingo.menu

import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object PlaceStructure {

    fun placeStructure(world: ServerWorld, pos: BlockPos, identifier: Identifier) {
        val template = world.structureTemplateManager.getTemplate(identifier).get()
        val placement = StructurePlacementData()

        template.place(
            world,
            pos,
            pos,
            placement,
            StructureBlockBlockEntity.createRandom(world.seed),
            2
        )
    }

}