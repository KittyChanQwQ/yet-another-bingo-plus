package me.jfenn.bingo.menu

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.config.TimeLimit
import me.jfenn.bingo.config.WinCondition
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.team.BingoTeam
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object MenuService {

    fun spawn() {
        placeStructure(lobbyWorld, BlockPos(-24, -2, -24), Identifier.of("bingo", "lobby")!!)
        spawnMenuEntities(Vec3d(-4.0, 3.0, -4.0))
    }

    private fun spawnMenuEntities(pos: Vec3d) {
        val startButton = ButtonEntity(
            position = pos.add(9.0, -1.0, 0.0),
            text = "START",
            isActive = BingoGame.config.isValid(),
        ).apply {
            onClick {
                if (BingoGame.config.isValid())
                    BingoGame.start()
            }
        }

        MenuEntity(
            title = "Win Condition",
            options = WinCondition.values()
                .map { it.name.replace('_', ' ') },
            selectedIndex = WinCondition.values()
                .indexOfFirst { it == BingoGame.config.winCondition },
            position = pos,
        ).onChange {
            BingoGame.config.winCondition = WinCondition.values()[it]
        }

        MenuEntity(
            title = "Time Limit",
            options = TimeLimit.values()
                .map { it.name.replace('_', ' ').trim() },
            selectedIndex = TimeLimit.values()
                .indexOfFirst { it == BingoGame.config.timeLimit },
            position = pos.add(2.0, 0.0, 0.0),
        ).onChange {
            BingoGame.config.timeLimit = TimeLimit.values()[it]
        }

        ItemDistributionEntity(
            position = pos.add(5.5, 0.0, 0.0),
            itemDistribution = BingoGame.config.itemDistribution,
        ).onChange {
            BingoGame.config.itemDistribution = it
            startButton.isActive = BingoGame.config.isValid()
        }

        TeamEntity(
            team = BingoTeam.ORANGE,
            position = Vec3d(-16.5, 4.0, -3.5),
        )

        TeamEntity(
            team = BingoTeam.GREEN,
            position = Vec3d(-14.5, 8.0, -19.5),
        )

        TeamEntity(
            team = BingoTeam.PINK,
            position = Vec3d(5.5, 5.0, -15.5),
        )

        TeamEntity(
            team = BingoTeam.BLUE,
            position = Vec3d(16.5, 3.0, -8.5),
        )

        TeamEntity(
            team = BingoTeam.YELLOW,
            position = Vec3d(18.5, 3.0, 6.5),
        )

        TeamEntity(
            team = BingoTeam.AQUA,
            position = Vec3d(16.5, 5.0, 17.5),
        )

        TeamEntity(
            team = BingoTeam.GRAY,
            position = Vec3d(-1.5, 1.0, 18.5),
        )

        TeamEntity(
            team = BingoTeam.RED,
            position = Vec3d(-16.5, 3.0, 14.5),
        )
    }

    private fun placeStructure(world: ServerWorld, pos: BlockPos, identifier: Identifier) {
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