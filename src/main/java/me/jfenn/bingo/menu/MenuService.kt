package me.jfenn.bingo.menu

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.Sounds
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.TimeLimit
import me.jfenn.bingo.config.WinCondition
import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.map.Color
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.team.BingoTeam
import me.jfenn.bingo.utils.sendWarningMessage
import me.jfenn.bingo.utils.toNbt
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sin

object MenuService {

    fun spawn(config: BingoConfig) {
        placeStructure(lobbyWorld, BlockPos(-24, -2, -24), Identifier.of("bingo", "lobby")!!)
        spawnMenuEntities(Vec3d(-4.0, 2.5, -4.0), config)
        spawnPlayerCount(Vec3d(0.0, 3.5, -4.0))
    }

    private fun spawnMenuEntities(pos: Vec3d, config: BingoConfig) {
        val startButton = ButtonEntity(
            position = pos.add(9.0, -MENU_ITEM_OFFSET, 0.0),
            text = "START",
            isActive = config.isValid(),
        ).apply {
            onClick {
                if (!config.isValid()) {
                    it.sendWarningMessage("Item distribution must add up to 25!")
                    return@onClick
                }

                val isEveryPlayerOnATeam = minecraftServer.playerManager.playerList
                    .all { player -> BingoTeam.values().any { team -> player.isTeamPlayer(team.team()) } }

                if (!isEveryPlayerOnATeam && !it.isSneaking) {
                    it.sendWarningMessage("Some players have not picked a team! Run /shuffleteams or sneak+click to ignore.")
                    return@onClick
                }

                BingoGame.start()
            }
        }

        spawnMenu(
            title = "Game Mode",
            value = config.gameMode,
            position = pos.add(-2.0, 0.0, 0.0),
        ).onChange {
            config.gameMode = BingoMode.values()[it]
        }

        spawnMenu(
            title = "Win Condition",
            value = config.winCondition,
            position = pos,
        ).onChange {
            config.winCondition = WinCondition.values()[it]
        }

        spawnMenu(
            title = "Time Limit",
            value = config.timeLimit,
            position = pos.add(2.0, 0.0, 0.0),
        ).onChange {
            config.timeLimit = TimeLimit.values()[it]
        }

        ItemDistributionEntity(
            position = pos.add(4.3, 0.0, 0.0),
            itemDistribution = BingoGame.config.itemDistribution,
        ).onChange {
            config.itemDistribution = it
            startButton.isActive = config.isValid()
        }

        spawnFeatures(pos.add(7.0, 0.0, 0.0), config)

        spawnTeamEntity(Vec3d(-16.5, 4.0, -3.5), BingoTeam.ORANGE)
        spawnTeamEntity(Vec3d(-14.5, 8.0, -19.5), BingoTeam.GREEN)
        spawnTeamEntity(Vec3d(5.5, 5.0, -15.5), BingoTeam.PINK)
        spawnTeamEntity(Vec3d(16.5, 3.0, -8.5), BingoTeam.BLUE)
        spawnTeamEntity(Vec3d(18.5, 3.0, 6.5), BingoTeam.YELLOW)
        spawnTeamEntity(Vec3d(16.5, 5.0, 17.5), BingoTeam.AQUA)
        spawnTeamEntity(Vec3d(-1.5, 1.0, 18.5), BingoTeam.GRAY)
        spawnTeamEntity(Vec3d(-16.5, 3.0, 14.5), BingoTeam.RED)
    }

    private inline fun <reified E : Enum<E>> spawnMenu(position: Vec3d, title: String, value: E): MenuEntity {
        val options = value.javaClass.enumConstants
        return MenuEntity(
            title = title,
            options =  options.map { it.name.replace('_', ' ') },
            selectedIndex = options.indexOfFirst { value == it },
            position = position,
        )
    }

    private fun spawnFeatures(position: Vec3d, config: BingoConfig) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Features")
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
            text = "Unlock Recipes",
            isActive = config.isUnlockRecipes,
        ).apply {
            onClick {
                config.isUnlockRecipes = !config.isUnlockRecipes
                this.isActive = config.isUnlockRecipes
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
            text = "Keep Inventory",
            isActive = config.isKeepInventory,
        ).apply {
            onClick {
                config.isKeepInventory = !config.isKeepInventory
                this.isActive = config.isKeepInventory
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + 2*MENU_ITEM_HEIGHT), 0.0),
            text = "Elytra",
            isActive = config.isElytra,
        ).apply {
            onClick {
                config.isElytra = !config.isElytra
                this.isActive = config.isElytra
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + 3*MENU_ITEM_HEIGHT), 0.0),
            text = "Allow PvP",
            isActive = BingoGame.config.isPvpEnabled,
        ).apply {
            onClick {
                config.isPvpEnabled = !config.isPvpEnabled
                this.isActive = config.isPvpEnabled
            }
        }
    }

    private fun spawnPlayerCount(position: Vec3d) {
        val textEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Player Count")
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        val update = {
            textEntity.readNbt(NbtCompound().apply {
                put("Pos", position.toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.literal("Player Count: ${minecraftServer.currentPlayerCount} / ${minecraftServer.maxPlayerCount}")
                ))
                putString("billboard", "fixed")
                putString("alignment", "center")
            })
        }

        PlayerEvents.onPlayerJoin { update() }
        PlayerEvents.onPlayerLeave { update() }
    }

    private fun spawnTeamEntity(position: Vec3d, team: BingoTeam) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, 1.5, 0.0).toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("${team.name} Team")
                            .formatted(team.formatting, Formatting.BOLD)
                    ))
                    putString("billboard", "vertical")
                    putString("alignment", "center")
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, 1.0, 0.0).toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Click to join!")
                    ))
                    putString("billboard", "vertical")
                    putString("alignment", "center")
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        val interactionListenerEntity = InteractionEntity(EntityType.INTERACTION, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, -1.0, 0.0).toNbt())
                    putFloat("width", 2f)
                    putFloat("height", 4f)
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        InteractionEntityEvents.onInteract(interactionListenerEntity) { player ->
            if (player is ServerPlayerEntity) {
                val color = Color.fromInt(team.formatting.colorValue ?: 0)
                val effect = DustParticleEffect(Vector3f(color.r / 255f, color.g / 255f, color.b / 255f), 1f)
                lobbyWorld.spawnParticles(player, effect, true, position.x, position.y, position.z, 10, 1.0, 1.0, 1.0, 0.2)

                Sounds.playTeamChanged(player)
            }

            team.addPlayer(player)
        }
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