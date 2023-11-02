package me.jfenn.bingo.menu

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.Sounds
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.config.BingoMode
import me.jfenn.bingo.config.Config
import me.jfenn.bingo.config.WinCondition
import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.event.PlayerEvents
import me.jfenn.bingo.lobbyWorld
import me.jfenn.bingo.map.Color
import me.jfenn.bingo.minecraftServer
import me.jfenn.bingo.spawn.SpawnItem
import me.jfenn.bingo.utils.sendWarningMessage
import me.jfenn.bingo.utils.toNbt
import net.minecraft.block.Blocks
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.particle.DustParticleEffect
import net.minecraft.screen.SimpleNamedScreenHandlerFactory
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

object MenuService {

    private val log = LoggerFactory.getLogger(this::class.java)

    var spawnPos: BlockPos = BlockPos.ORIGIN

    fun spawn(config: BingoConfig) {
        spawnLobbyStructure()
        spawnMenuEntities(Vec3d(-2.0, spawnPos.y + 2.5, -4.0), config)
        spawnPlayerCount(Vec3d(0.0, spawnPos.y + 3.5, -4.0))
    }

    private fun spawnMenuEntities(pos: Vec3d, config: BingoConfig) {
        val startButton = ButtonEntity(
            position = pos.add(8.0, -MENU_ITEM_OFFSET, 0.0),
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

        spawnKitMenu(
            position = pos.add(-5.2, 0.0, 0.0),
            config = config,
        )

        TeamsEntity(
            title = "Teams",
            position = pos.add(-3.5, 0.0, 0.0),
        )

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
            position = pos.add(2.2, 0.0, 0.0),
        ).onChange {
            config.winCondition = WinCondition.values()[it]
        }

        TimerEntity(
            title = "Time Limit",
            timerMinutesDefault = config.timerMinutes,
            position = pos.add(4.0, 0.0, 0.0),
        ).onChange {
            config.timerMinutes = it
        }

        ItemDistributionEntity(
            position = pos.add(6.0, 0.0, 0.0),
            itemDistribution = BingoGame.config.itemDistribution,
        ).onChange {
            config.itemDistribution = it
            startButton.isActive = config.isValid()
        }

        spawnFeaturesMenu(pos.add(0.0, 0.0, 0.0), config)
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

    private fun spawnFeaturesMenu(position: Vec3d, config: BingoConfig) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Features")
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                    putInt("background", 0)
                    putBoolean("shadow", true)
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

    private fun spawnKitMenu(position: Vec3d, config: BingoConfig) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.toNbt())
                    putString("text", Text.Serializer.toJson(
                        Text.literal("Spawn Kit")
                    ))
                    putString("billboard", "fixed")
                    putString("alignment", "center")
                    putInt("background", 0)
                    putBoolean("shadow", true)
                })
            }
            .also { lobbyWorld.spawnEntity(it) }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET), 0.0),
            text = "Player Items",
            isActive = config.isPlayerKit,
        ).apply {
            onClick {
                config.isPlayerKit = !config.isPlayerKit
                this.isActive = config.isPlayerKit
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT), 0.0),
            text = "Edit...",
        ).apply {
            onClick { player ->
                player.openHandledScreen(
                    SimpleNamedScreenHandlerFactory(
                        { syncId, inv, _ ->
                            InventoryScreenHandler(syncId, config.playerKitItems.flatMap { it.toItemStacks() }, inv).apply {
                                onClose { stacks ->
                                    config.playerKitItems = stacks.map { SpawnItem.fromItemStack(it) }
                                }
                            }
                        },
                        Text.literal("Player Items")
                    )
                )
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 3), 0.0),
            text = "Team Chest",
            isActive = config.isTeamKit,
        ).apply {
            onClick {
                config.isTeamKit = !config.isTeamKit
                this.isActive = config.isTeamKit
            }
        }

        ButtonEntity(
            position = position.add(0.0, -(MENU_ITEM_OFFSET + MENU_ITEM_HEIGHT * 4), 0.0),
            text = "Edit...",
        ).apply {
            onClick { player ->
                player.openHandledScreen(
                    SimpleNamedScreenHandlerFactory(
                        { syncId, inv, _ ->
                            InventoryScreenHandler(syncId, config.teamKitItems.flatMap { it.toItemStacks() }, inv).apply {
                                onClose { stacks ->
                                    config.teamKitItems = stacks.map { SpawnItem.fromItemStack(it) }
                                }
                            }
                        },
                        Text.literal("Team Chest")
                    )
                )
            }
        }
    }

    private fun spawnPlayerCount(position: Vec3d) {
        val textEntity = DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .also { lobbyWorld.spawnEntity(it) }

        val update = {
            textEntity.readNbt(NbtCompound().apply {
                put("Pos", position.toNbt())
                putString("text", Text.Serializer.toJson(
                    Text.empty()
                        .append("Player count: ")
                        .append(Text.literal("${minecraftServer.currentPlayerCount} / ${minecraftServer.maxPlayerCount}").formatted(Formatting.GREEN))
                ))
                putString("billboard", "fixed")
                putString("alignment", "center")
                putInt("background", 0)
                putBoolean("shadow", true)
            })
        }

        update()
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
                    putInt("background", 0)
                    putBoolean("shadow", true)
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

            TeamService.joinTeam(player, team)
        }
    }

    private fun spawnLobbyStructure() = measureTimeMillis {
        val lobbyDir = Config.configDir.resolve("lobby")
        if (!Files.exists(lobbyDir))
            lobbyDir.toFile().mkdirs()

        val lobbyFile = Files.list(lobbyDir)
            .map { it.name }
            .toList()
            .toMutableSet()
            .apply { add("lobby.nbt") }
            .random()

        log.info("Spawning lobby structure '$lobbyFile'")

        // get a random NBT structure from the config, and read as string
        val lobbyFileStream = lobbyDir.resolve(lobbyFile)
            .toFile()
            .takeIf { it.exists() }
            ?.inputStream()
            ?: run {
                // if there aren't any custom NBT files, read the default (lobby.nbt) from resources
                javaClass.getResourceAsStream("/bingo/lobby/lobby.nbt")
                    ?: throw RuntimeException("Unable to obtain lobby.nbt resource")
            }

        val lobbyNbt = NbtIo.readCompressed(lobbyFileStream)
        placeStructure(lobbyWorld, BlockPos(0, 24, 0), lobbyNbt)

        // Search for team join entities
        for (x in -24..24) {
            for (y in -24..24) {
                for (z in -24..24) {
                    val isLodestone = lobbyWorld.getBlockState(BlockPos(x, y-1, z)).block == Blocks.LODESTONE
                    if (!isLodestone) continue

                    val blockPos = BlockPos(x, y, z)
                    val blockState = lobbyWorld.getBlockState(blockPos)

                    val team = BingoTeam.values() .find { it.carpetBlock == blockState.block } ?: continue
                    spawnTeamEntity(Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5), team)
                    lobbyWorld.setBlockState(blockPos, Blocks.AIR.defaultState)
                }
            }
        }

        // Search for spawnpoint
        val spawnY = (0..24)
            .asSequence()
            .filter { y -> lobbyWorld.getBlockState(BlockPos(0, y, 0)).isSolidBlock(lobbyWorld, BlockPos.ORIGIN) }
            .max()

        spawnPos = BlockPos(0, spawnY+1, 0)
        log.info("Set lobby spawnpoint to $spawnPos")
    }.also {
        log.info("Done (${it}ms)!")
    }

    private fun placeStructure(world: ServerWorld, pos: BlockPos, nbt: NbtCompound) {
        val template = world.structureTemplateManager.createTemplate(nbt)
        val placement = StructurePlacementData()

        val posOffset = template.size.run { pos.add(-x/2, -y/2, -z/2) }

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