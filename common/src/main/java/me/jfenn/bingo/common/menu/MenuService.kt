package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.IStructureManager
import me.jfenn.bingo.common.Bingo
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.card.BingoTeam
import me.jfenn.bingo.common.card.TeamService
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.config.Config
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.utils.text.TextInfo
import me.jfenn.bingo.common.utils.toNbt
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.Blocks
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

object MenuService : KoinComponent {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val structureManager by inject<IStructureManager>()

    fun init() {
        GameState.PREGAME.onEnter {
            // this *shouldn't* happen... just being safe to prevent double-entities
            if (menu != null) {
                log.error("BUG: GameState.PREGAME onEnter handler has been invoked already!")
                return@onEnter
            }

            // always load the spawn chunks
            lobbyWorld.chunkManager.addTicket(ChunkTicketType.START, ChunkPos(0, 0), 11, Unit.INSTANCE)

            spawnLobbyStructure(this, lobbyWorld)
            menu = MenuInstance(this, state.lobbySpawnPos)
        }

        ServerEntityEvents.ENTITY_LOAD.register { entity, world ->
            val ctx = Bingo.getContext(world.server) ?: return@register
            val bingoTag = entity.commandTags.find { it.startsWith("bingo-") }
                ?: return@register

            if (world == ctx.lobbyWorld && !entity.isPlayer && bingoTag != ctx.menu?.instanceTag) {
                log.warn("Discarding an existing lobby world entity: ${entity.type}")
                entity.discard()
            }
        }

        Bingo.onGameTick {
            menu?.tick()
        }

        GameState.PLAYING.onEnter {
            menu?.cleanup()
            menu = null
        }

        ServerPlayConnectionEvents.JOIN.register { _, _, server ->
            val ctx = Bingo.getContext(server)
            ctx?.menu?.markDirty()
        }

        ServerPlayConnectionEvents.DISCONNECT.register { _, server ->
            val ctx = Bingo.getContext(server)
            ctx?.menu?.markDirty()
        }
    }

    private fun spawnTeamEntity(lobbyWorld: ServerWorld, position: Vec3d, team: BingoTeam) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, 1.5, 0.0).toNbt())
                    putString("text", TextInfo.toJsonString(
                        TextInfo.literal("${team.name} Team")
                            .formatted(team.textColor)
                            .bold()
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
                    putString("text", TextInfo.toJsonString(
                        TextInfo.literal("Click to join!")
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
            val color = Color.fromInt(team.formatting.colorValue ?: 0)
            val effect = DustParticleEffect(Vector3f(color.r / 255f, color.g / 255f, color.b / 255f), 1f)
            (player.world as? ServerWorld)?.spawnParticles(player, effect, true, position.x, position.y, position.z, 10, 1.0, 1.0, 1.0, 0.2)

            Sounds.playTeamChanged(player)
            TeamService.joinTeam(player, team)
        }
    }

    private fun spawnLobbyStructure(ctx: BingoContext, lobbyWorld: ServerWorld) = measureTimeMillis {
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

        structureManager.placeStructure(lobbyWorld, BlockPosition(0, 25, 0), lobbyFileStream)

        // Search for team join entities
        for (x in -24..24) {
            for (y in -24..24) {
                for (z in -24..24) {
                    val isLodestone = lobbyWorld.getBlockState(BlockPos(x, y-1, z)).block == Blocks.LODESTONE
                    if (!isLodestone) continue

                    val blockPos = BlockPos(x, y, z)
                    val blockState = lobbyWorld.getBlockState(blockPos)

                    val team = BingoTeam.entries.find { it.carpetBlock == blockState.block } ?: continue
                    spawnTeamEntity(lobbyWorld, Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5), team)
                    lobbyWorld.setBlockState(blockPos, Blocks.AIR.defaultState)
                }
            }
        }

        // Search for spawnpoint
        val spawnY = (0..24)
            .asSequence()
            .filter { y -> lobbyWorld.getBlockState(BlockPos(0, y, 0)).isSolidBlock(lobbyWorld, BlockPos.ORIGIN) }
            .max()

        ctx.state.lobbySpawnPos = BlockPosition(0, spawnY+1, 0)
        log.info("Set lobby spawnpoint to ${ctx.state.lobbySpawnPos}")
    }.also {
        log.info("Done (${it}ms)!")
    }

}