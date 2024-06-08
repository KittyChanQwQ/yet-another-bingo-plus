package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.ILevelStorage
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.common.LOBBY_WORLD_ID
import me.jfenn.bingo.common.MOD_ID_BINGO
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.config.BlockPosition
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.map.Color
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.measureTime
import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.generated.StringKey
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.DisplayEntity
import net.minecraft.entity.decoration.InteractionEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.DustParticleEffect
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.text.Texts
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Heightmap
import net.minecraft.world.chunk.ChunkStatus
import org.joml.Vector3f
import org.slf4j.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipInputStream

class MenuController(
    events: ScopedEvents,
    scopeManager: ScopeManager,
    private val log: Logger,
    private val server: MinecraftServer,
    private val interactionEntityEvents: InteractionEntityEvents,
    private val state: BingoState,
    private val teamService: TeamService,
    private val text: TextProvider,
    private val playerManager: IPlayerManager,
    private val levelStorage: ILevelStorage,
) : BingoComponent() {

    private var menu: MenuInstance? = null
    private val lobbyWorld get() = server.lobbyWorld!!
    private val koinScope = scopeManager.getScope(server)!!

    private val instanceTag = "bingo-${UUID.randomUUID()}"

    init {
        prepareLobbyFiles()

        events.onEnter(GameState.PREGAME)  {
            // this *shouldn't* happen... just being safe to prevent double-entities
            if (menu != null) {
                log.error("BUG: GameState.PREGAME onEnter handler has been invoked already!")
                return@onEnter
            }

            val lobbySpawnPos = getLobbySpawnPosition()
            state.lobbySpawnPos = lobbySpawnPos

            val menuPos = getMenuPosition()
            if (menuPos != null) {
                log.info("[MenuController] Spawning lobby menu at $menuPos")
                val menuInstance = MenuInstance(
                    log = log,
                    koinScope = koinScope,
                    server = server,
                    interactionEntityEvents = interactionEntityEvents,
                    menuPos = menuPos,
                    instanceTag = instanceTag,
                )
                menu = menuInstance
            } else {
                log.warn("[MenuController] Not spawning menu entities as the lobby has no menu position.")
            }

            spawnTeamEntities()
        }

        events.onEntityLoad { (entity, world) ->
            val bingoTag = entity.commandTags.find { it.startsWith("bingo-") }
                ?: return@onEntityLoad

            if (world == lobbyWorld && !entity.isPlayer && bingoTag != instanceTag) {
                log.warn("Discarding an existing lobby world entity: ${entity.type}")
                entity.discard()
            }
        }

        events.onGameTick {
            menu?.tick()
        }

        events.onEnter(GameState.PLAYING) {
            menu?.cleanup()
            menu = null
        }

        events.onChangeConfig {
            menu?.markDirty()
        }

        events.onPlayerJoin {
            menu?.markDirty()
        }

        events.onPlayerLeave {
            menu?.markDirty()
        }
    }

    private fun prepareLobbyFiles() = log.measureTime("[MenuController] Preparing the BINGO lobby...") {
        val storageDir = levelStorage.getLevelSaveDir(LOBBY_WORLD_ID)
            ?: run {
                log.error("Unable to find world storage dir for $LOBBY_WORLD_ID")
                return@measureTime
            }

        this::class.java.getResourceAsStream("/$MOD_ID_BINGO/lobby_world.zip")?.use { stream ->
            ZipInputStream(stream).use { zipStream ->
                while (true) {
                    val entry = zipStream.nextEntry ?: break
                    if (entry.isDirectory) continue

                    val entryName = entry.name.substringAfter('/')

                    if (
                        entryName.startsWith("entities/") ||
                        entryName.startsWith("region/")
                    ) {
                        val outPath = storageDir.resolve(entryName)
                        if (!outPath.startsWith(storageDir)) {
                            log.error("[MenuController] Tried to create a file outside of the world location: $outPath")
                            continue
                        }

                        Files.createDirectories(outPath.parent)

                        log.debug("Copying file -> {}", outPath)
                        try {
                            Files.copy(zipStream, outPath, StandardCopyOption.REPLACE_EXISTING)
                        } catch (e: IOException) {
                            log.error("[MenuController] Error copying lobby file $entryName", e)
                        }
                    }

                    zipStream.closeEntry()
                }
            }

        } ?: run {
            log.error("[MenuController] Unable to find bingo lobby_world.zip in resources")
        }
    }

    private fun searchLobbyBlocks() = sequence {
        for (x in -24..24) {
            for (y in 0..48) {
                for (z in -24..24) {
                    yield(BlockPosition(x, y, z))
                }
            }
        }
    }

    private fun getMenuPosition(): BlockPosition? = log.measureTime("[MenuController] Locating menu position...") {
        // Find a column of three vertical wall_sign blocks for the menu position
        for (pos in searchLobbyBlocks()) {
            val blocks = listOf(
                pos.move(y = 1).toBlockPos(),
                pos.toBlockPos(),
                pos.move(y = -1).toBlockPos()
            )
            val isMenuPos = blocks.all {
                lobbyWorld.getBlockState(it).block == Blocks.OAK_WALL_SIGN
            }

            if (isMenuPos) {
                for (blockPos in blocks) {
                    lobbyWorld.setBlockState(blockPos, Blocks.AIR.defaultState)
                }
                return@measureTime pos
            }
        }

        null
    }.also { log.info("Set menu position to $it") }

    private fun getLobbySpawnPosition(): BlockPosition = log.measureTime("[MenuController] Locating spawn position...") {
        // If a player head is in the lobby structure, use it as the player spawn
        for (pos in searchLobbyBlocks()) {
            val blockPos = pos.toBlockPos()
            val isPlayerHead = lobbyWorld.getBlockState(blockPos).block == Blocks.PLAYER_HEAD
            if (isPlayerHead) {
                lobbyWorld.setBlockState(blockPos, Blocks.AIR.defaultState)
                return@measureTime pos
            }
        }

        // otherwise, find the highest block at 0,0
        val chunk = lobbyWorld.getChunk(0, 0, ChunkStatus.FULL)
        val heightmap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING)
        val spawnY = heightmap.get(0, 0)

        BlockPosition(0, spawnY, 0)
    }.also { log.info("Set lobby spawnpoint to $it") }

    private val teamPositions = mutableMapOf<BingoTeamKey, BlockPos>()

    private fun spawnTeamEntities() = log.measureTime("[MenuController] Locating team entities...") {
        // Search for team join entities
       for ((x, y, z) in searchLobbyBlocks()) {
            val isLodestone = lobbyWorld.getBlockState(BlockPos(x, y-1, z)).block == Blocks.LODESTONE
            if (!isLodestone) continue

            val blockPos = BlockPos(x, y, z)
            val blockState = lobbyWorld.getBlockState(blockPos)

            val team = BingoTeam.TEAM_BLOCKS[blockState.block] ?: continue
            teamPositions[team.key] = blockPos
            spawnTeamEntity(lobbyWorld, Vec3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5), team, instanceTag)
            lobbyWorld.setBlockState(blockPos, Blocks.AIR.defaultState)
        }
    }

    private fun spawnTeamEntity(
        lobbyWorld: ServerWorld,
        position: Vec3d,
        team: BingoTeam,
        instanceTag: String,
    ) {
        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, 1.5, 0.0).toNbt())
                    putString("text", text.serialized(
                        text.string(StringKey.TeamName, Text.empty().append("${team.symbol} ").append(text.string(team.name)))
                            .formatted(team.formatting)
                            .let { Texts.bracketed(it) }
                    ))
                    putString("billboard", "vertical")
                    putString("alignment", "center")
                    putInt("background", 0)
                    putBoolean("shadow", true)
                })
            }
            .also { it.addCommandTag(instanceTag) }
            .also { lobbyWorld.spawnEntity(it) }

        DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, 1.0, 0.0).toNbt())
                    putString("text", text.serialized(
                        text.string(StringKey.LobbyClickToJoin)
                    ))
                    putString("billboard", "vertical")
                    putString("alignment", "center")
                })
            }
            .also { it.addCommandTag(instanceTag) }
            .also { lobbyWorld.spawnEntity(it) }

        val interactionListenerEntity = InteractionEntity(EntityType.INTERACTION, lobbyWorld)
            .apply {
                readNbt(NbtCompound().apply {
                    put("Pos", position.add(0.0, -1.0, 0.0).toNbt())
                    putFloat("width", 2f)
                    putFloat("height", 4f)
                })
            }
            .also { it.addCommandTag(instanceTag) }
            .also { lobbyWorld.spawnEntity(it) }

        interactionEntityEvents.onInteract(interactionListenerEntity) { player ->
            val color = Color.fromInt(team.formatting.colorValue ?: 0)
            val effect = DustParticleEffect(Vector3f(color.r / 255f, color.g / 255f, color.b / 255f), 1f)
            (player.world as? ServerWorld)?.spawnParticles(player, effect, true, position.x, position.y, position.z, 10, 1.0, 1.0, 1.0, 0.2)

            Sounds.playTeamChanged(playerManager.forPlayer(player))
            teamService.joinTeam(player, team)
        }
    }

}