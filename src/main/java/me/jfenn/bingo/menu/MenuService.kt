package me.jfenn.bingo.menu

import me.jfenn.bingo.Bingo
import me.jfenn.bingo.Sounds
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.card.TeamService
import me.jfenn.bingo.config.BlockPosition
import me.jfenn.bingo.config.Config
import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.map.Color
import me.jfenn.bingo.state.BingoContext
import me.jfenn.bingo.state.GameState
import me.jfenn.bingo.utils.toNbt
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.Blocks
import net.minecraft.block.entity.StructureBlockBlockEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtTagSizeTracker
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.structure.StructurePlacementData
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Unit
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

object MenuService {

    private val log = LoggerFactory.getLogger(this::class.java)

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
                    putString("text", Text.Serialization.toJsonString(
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
                    putString("text", Text.Serialization.toJsonString(
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
                (player.world as? ServerWorld)?.spawnParticles(player, effect, true, position.x, position.y, position.z, 10, 1.0, 1.0, 1.0, 0.2)

                Sounds.playTeamChanged(player)
            }

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

        val lobbyNbt = lobbyFileStream.use { NbtIo.readCompressed(it, NbtTagSizeTracker(Long.MAX_VALUE, 512)) }
        placeStructure(lobbyWorld, BlockPos(0, 24, 0), lobbyNbt)

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