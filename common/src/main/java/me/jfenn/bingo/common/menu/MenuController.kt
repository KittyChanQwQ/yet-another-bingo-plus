package me.jfenn.bingo.common.menu

import me.jfenn.bingo.api.state.GameState
import me.jfenn.bingo.common.Sounds
import me.jfenn.bingo.common.data.ScopedData
import me.jfenn.bingo.common.event.InteractionEntityEvents
import me.jfenn.bingo.common.event.ScopedEvents
import me.jfenn.bingo.common.game.GameWarningEvent
import me.jfenn.bingo.common.lobbyWorld
import me.jfenn.bingo.common.menu.poi.MenuPoi
import me.jfenn.bingo.common.menu.poi.MenuPoiState
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeam
import me.jfenn.bingo.common.team.BingoTeamKey
import me.jfenn.bingo.common.team.BingoTeamPreset
import me.jfenn.bingo.common.team.TeamService
import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.common.utils.plus
import me.jfenn.bingo.generated.StringKey
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.block.BlockPosition
import me.jfenn.bingo.platform.block.ChunkPosition
import me.jfenn.bingo.platform.event.IEventBus
import me.jfenn.bingo.platform.event.game.GameResetEvent
import me.jfenn.bingo.platform.event.model.EntityLoadEvent
import me.jfenn.bingo.platform.event.model.TickEvent
import me.jfenn.bingo.platform.world.IChunkHeightmap
import org.joml.Matrix4d
import org.joml.Vector3d
import org.koin.core.scope.Scope
import org.slf4j.Logger
import java.util.*

internal class MenuController(
    events: ScopedEvents,
    eventBus: IEventBus,
    private val log: Logger,
    private val koinScope: Scope,
    private val entityManager: IEntityManager,
    private val interactionEntityEvents: InteractionEntityEvents,
    private val state: BingoState,
    private val menuPoiState: MenuPoiState,
    private val data: ScopedData,
    private val teamService: TeamService,
    private val text: TextProvider,
    private val particleFactory: me.jfenn.bingo.platform.particle.IParticleFactory,
    private val serverWorldFactory: IServerWorldFactory,
    private val taskExecutor: IExecutors.IServerTaskExecutor,
) : BingoComponent() {

    private var menu: MenuInstance? = null

    private val instanceTag = "bingo-${UUID.randomUUID()}"

    init {
        events.onEnter(GameState.PREGAME)  {
            // this *shouldn't* happen... just being safe to prevent double-entities
            if (menu != null) {
                log.error("BUG: GameState.PREGAME onEnter handler has been invoked already!")
                return@onEnter
            }

            if (!state.isLobbyMode) {
                log.info("[MenuController] Not spawning lobby, as isLobbyMode=false")
                return@onEnter
            }

            val lobbyWorld = serverWorldFactory.lobbyWorld
            if (lobbyWorld == null) {
                log.error("[MenuController] Not spawning lobby, as the dimension does not exist")
                return@onEnter
            }

            val (lobbySpawnPos, lobbySpawnYaw) = getLobbySpawnPosition(lobbyWorld)
            state.lobbySpawnPos = lobbySpawnPos
            state.lobbySpawnYaw = lobbySpawnYaw

            val menuPos = getMenuPosition(lobbyWorld)
            if (menuPos != null) {
                log.info("[MenuController] Spawning lobby menu at ${menuPos.getTranslation(Vector3d())}")
                val menuInstance = MenuInstance(
                    log = log,
                    koinScope = koinScope,
                    world = lobbyWorld,
                    entityManager = entityManager,
                    interactionEntityEvents = interactionEntityEvents,
                    matrix = menuPos,
                    instanceTag = instanceTag,
                )
                menu = menuInstance
            } else {
                log.warn("[MenuController] Not spawning menu entities as the lobby has no menu position.")
            }

            spawnTeamEntities(lobbyWorld)
        }

        eventBus.register(EntityLoadEvent) {
            val lobbyWorld = serverWorldFactory.lobbyWorld ?: return@register
            val bingoTag = it.entity.commandTags.find { it.startsWith("bingo-") }
                ?: return@register

            if (it.world == lobbyWorld && !it.entity.isPlayer && bingoTag != instanceTag) {
                log.warn("Discarding an existing lobby world entity: ${it.entity.type}")
                it.entity.discard()
            }
        }

        eventBus.register(TickEvent.Start) {
            menu?.tick()
        }

        events.onStateChange { (_, to) ->
            if (to != GameState.PREGAME) {
                despawnMenu()
            }
        }

        events.onChangeOptions {
            menu?.markDirty()
        }

        events.onPlayerJoin {
            menu?.markDirty()
        }

        events.onPlayerDisconnect {
            menu?.markDirty()
        }

        eventBus.register(GameWarningEvent) {
            if (it.severe) {
                state.menu = MenuState.Warnings(it.warnings)
                menu?.markDirty()
            }
        }

        eventBus.register(GameResetEvent.BeforeClose) {
            despawnMenu()
        }
    }

    private fun despawnMenu() {
        menu?.cleanup()
        menu = null

        // discard team picker entities which aren't part of the menu
        val lobbyWorld = serverWorldFactory.lobbyWorld ?: return
        entityManager.iterateEntities(lobbyWorld)
            .filter { !it.isPlayer && it.commandTags.contains(instanceTag) }
            .toList()
            .forEach { it.discard() }
    }

    private fun getMenuPosition(lobbyWorld: IServerWorld): Matrix4d? {
        val menuPoi = menuPoiState.poi.filterIsInstance<MenuPoi.Menu>().firstOrNull() ?: return null

        taskExecutor.execute {
            // schedule the blockstate change, as the chunk might not be loaded yet
            for (y in -1..1) {
                lobbyWorld.setBlockState(
                    menuPoi.pos.move(y = y),
                    lobbyWorld.createBlockState("minecraft:air"),
                )
            }
        }

        val matrix4d = Matrix4d()
        matrix4d.translate(menuPoi.pos.toVector3f())
        matrix4d.rotateY(menuPoi.yaw)

        return matrix4d
    }

    private fun getLobbySpawnPosition(lobbyWorld: IServerWorld): MenuPoi.Spawn {
        val spawnPos = menuPoiState.poi.filterIsInstance<MenuPoi.Spawn>().firstOrNull()
        if (spawnPos != null) {
            taskExecutor.execute {
                // schedule the blockstate change, as the chunk might not be loaded yet
                lobbyWorld.setBlockState(spawnPos.pos, lobbyWorld.createBlockState("minecraft:air"))
            }
            return spawnPos
        }

        // otherwise, find the highest block at 0,0
        val chunk = lobbyWorld.getChunkSync(ChunkPosition(0, 0))
        val heightmap = chunk.getHeightmap(IChunkHeightmap.Type.MOTION_BLOCKING)
        val spawnY = heightmap.get(0, 0)

        return MenuPoi.Spawn(BlockPosition(0, spawnY, 0), 180f)
    }

    private fun spawnTeamEntities(lobbyWorld: IServerWorld) {
        for (teamPoi in menuPoiState.poi.filterIsInstance<MenuPoi.Team>()) {
            val teamKey = teamPoi.key
            val blockPos = teamPoi.pos
            val team = data.teamPresets[teamKey] ?: continue
            spawnTeamEntity(lobbyWorld, Vector3d(blockPos.x + 0.5, blockPos.y.toDouble(), blockPos.z + 0.5), teamKey, team, instanceTag)

            taskExecutor.execute {
                // schedule the blockstate change, as the chunk might not be loaded yet
                lobbyWorld.setBlockState(blockPos, lobbyWorld.createBlockState("minecraft:air"))
            }
        }
    }

    private fun spawnTeamEntity(
        lobbyWorld: IServerWorld,
        position: Vector3d,
        teamKey: BingoTeamKey,
        teamPreset: BingoTeamPreset,
        instanceTag: String,
    ) {
        entityManager.createEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                pos = position + Vector3d(0.0, 1.5, 0.0)
                value = BingoTeam.fromPreset(teamKey, teamPreset)
                    .getName(
                        textProvider = text,
                        symbol = true,
                    )
                billboard = ITextDisplayEntity.Billboard.VERTICAL
                alignment = ITextDisplayEntity.TextAlignment.CENTER
                background = 0
                shadow = true
            }
            .also { it.commandTags += instanceTag }
            .also { entityManager.spawnEntity(lobbyWorld, it) }

        entityManager.createEntity(EntityType.TEXT_DISPLAY, lobbyWorld)
            .apply {
                pos = position + Vector3d(0.0, 1.0, 0.0)
                value = text.string(StringKey.LobbyClickToJoin)
                billboard = ITextDisplayEntity.Billboard.VERTICAL
                alignment = ITextDisplayEntity.TextAlignment.CENTER
            }
            .also { it.commandTags += instanceTag }
            .also { entityManager.spawnEntity(lobbyWorld, it) }

        val interactionListenerEntity = entityManager.createEntity(EntityType.INTERACTION, lobbyWorld)
            .apply {
                pos = position + Vector3d(0.0, -1.0, 0.0)
                width = 2f
                height = 4f
            }
            .also { it.commandTags += instanceTag }
            .also { entityManager.spawnEntity(lobbyWorld, it) }

        interactionEntityEvents.onInteract(interactionListenerEntity) { player ->
            val team = BingoTeam.fromPreset(teamKey, teamPreset)

            particleFactory.createDustParticle(team.textColor.colorValue ?: 0, 1f)
                .spawn(player, position, 10, Vector3d(1.0), 0.2)

            Sounds.playTeamChanged(player)
            teamService.joinTeam(player, team)
        }
    }

}