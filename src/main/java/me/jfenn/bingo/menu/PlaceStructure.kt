package me.jfenn.bingo.menu

import me.jfenn.bingo.minecraftServer
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.client.gui.screen.ingame.JigsawBlockScreen
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.PlaceCommand
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePiece
import net.minecraft.structure.StructurePlacementData
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.world.gen.structure.Structure

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

        /*
        val structureRegistry = minecraftServer.registryManager.get(RegistryKeys.STRUCTURE)
        structureRegistry.ids
            .forEach { println(it) }

        val structure = structureRegistry.get(identifier)!!

        val chunkGenerator = world.chunkManager.chunkGenerator
        val start = structure.createStructureStart(
            minecraftServer.registryManager,
            chunkGenerator,
            chunkGenerator.biomeSource,
            world.chunkManager.noiseConfig,
            world.structureTemplateManager,
            world.seed,
            ChunkPos(pos),
            0,
            world
        ) { _ ->
            true
        }

        val chunkMin = ChunkPos(
            ChunkSectionPos.getSectionCoord(start.boundingBox.minX),
            ChunkSectionPos.getSectionCoord(start.boundingBox.minZ)
        )
        val chunkMax = ChunkPos(
            ChunkSectionPos.getSectionCoord(start.boundingBox.maxX),
            ChunkSectionPos.getSectionCoord(start.boundingBox.maxZ)
        )

        ChunkPos.stream(chunkMin, chunkMax).forEach { chunkPos ->
            start.place(
                world,
                world.structureAccessor,
                chunkGenerator,
                world.getRandom(),
                BlockBox(
                    chunkPos.startX,
                    world.bottomY,
                    chunkPos.startZ,
                    chunkPos.endX,
                    world.topY,
                    chunkPos.endZ
                ),
                chunkPos
            )
        }*/
    }

}