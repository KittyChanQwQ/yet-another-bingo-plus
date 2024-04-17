package me.jfenn.bingo.client.common.world

import com.google.common.collect.ImmutableList
import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.common.BINGO_WORLD_PREFIX
import me.jfenn.bingo.common.datapack.DatapackService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ConfirmScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.resource.DataConfiguration
import net.minecraft.resource.DataPackSettings
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import org.slf4j.Logger
import kotlin.io.path.name

class BingoWorldManager(
    private val log: Logger,
    private val buttonService: IButtonService,
    private val worldState: BingoWorldState,
) : BingoComponent() {

    fun createBingoWorld(client: MinecraftClient, parent: Screen) {
        worldState.state = ScreenState.CreateBingoWorld
        CreateWorldScreen.create(client, parent)
    }

    init {
        ScopeManager.onCleanup { scope ->
            val state = scope.get<BingoState>()
            val server = scope.get<MinecraftServer>()

            if (state.state != GameState.PLAYING && server.saveProperties.levelName.startsWith(BINGO_WORLD_PREFIX)) {
                // If the game is not in progress, delete it!
                log.info("Deleting closed BINGO world save")

                val dirName = server.getSavePath(WorldSavePath.ROOT).parent.name
                val client = MinecraftClient.getInstance()
                client.levelStorage.createSessionWithoutSymlinkCheck(dirName).use {
                    it.deleteSessionLock()
                }
            }
        }

        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            if (screen is CreateWorldScreen && worldState.state == ScreenState.CreateBingoWorld) {
                // Add a prefix to the names of bingo worlds
                if (!screen.worldCreator.worldName.startsWith(BINGO_WORLD_PREFIX)) {
                    screen.worldCreator.worldName = "$BINGO_WORLD_PREFIX ${screen.worldCreator.worldName}"
                }
            }
        }

        ScreenEvents.AFTER_INIT.register { client, screen, _, height ->
            val state = worldState.state

            if (screen is TitleScreen) {
                if (state == ScreenState.ImmediatelyReopenBingoWorld) {
                    createBingoWorld(client, screen)
                    return@register
                }

                worldState.state = null

                val buttons = Screens.getButtons(screen)
                buttons.add(
                    buttonService.createButton(
                        x = 10,
                        y = height - 70,
                        width = 80,
                        height = 50,
                        texture = Identifier.of("minecraft", "bingo/button"),
                        focusedTexture = Identifier.of("minecraft", "bingo/button_focused"),
                        onClick = { createBingoWorld(client, screen) },
                        message = Text.literal("BINGO!"),
                    )
                )
            }

            log.debug(
                "init screen [{}]: {} isApplyingLobbyDataPack={}",
                screen::class.java.simpleName,
                state,
                worldState.isApplyingLobbyDataPack
            )

            if (worldState.isApplyingLobbyDataPack) {
                // this flag prevents infinite recursion, as there is a setScreen call in applyDataPacks
                return@register
            }

            if (screen is CreateWorldScreen && (state == ScreenState.CreateBingoWorld || state == null)) {
                // Copy the built-in lobby data pack to the world creator's temp dir
                // - we want this to happen on every world creation, so that the pack is available
                //   even if the user has not specifically pressed the "BINGO!" button.
                screen.dataPackTempDir?.let {
                    DatapackService.copyDataPack(it.resolve("bingo-lobby.zip"))
                }

                val pair = screen.getScannedPack(screen.worldCreator.generatorOptionsHolder.dataConfiguration)
                    ?: return@register

                // this flag prevents infinite recursion, as there is a setScreen call in applyDataPacks
                worldState.isApplyingLobbyDataPack = true
                screen.applyDataPacks(pair.second, false) {}
                worldState.isApplyingLobbyDataPack = false

                if (state == ScreenState.CreateBingoWorld) {
                    // actually enable the bingo lobby datapack
                    pair.second.setEnabledProfiles(
                        (pair.second.enabledNames + "file/bingo-lobby.zip")
                            .reversed()
                    )

                    val enabled = ImmutableList.copyOf(pair.second.enabledNames)
                    val disabled = pair.second.names.filter { !enabled.contains(it) }
                    val dataConfiguration = DataConfiguration(
                        DataPackSettings(enabled, disabled),
                        screen.worldCreator.generatorOptionsHolder.dataConfiguration().enabledFeatures()
                    )

                    worldState.state = ScreenState.OpenBingoWorld
                    screen.validateDataPacks(pair.second, dataConfiguration) {}

                    return@register
                }
            }

            if (screen is CreateWorldScreen && state == ScreenState.OpenBingoWorld) {
                // Once the world creation screen is reached, immediately press the
                // create world button

                val createWorldButton = Screens.getButtons(screen)
                    .find {
                        val content = it.message.content as? TranslatableTextContent
                        content?.key == "selectWorld.create"
                    }

                worldState.state = ScreenState.ConfirmExperimentalFeatures
                createWorldButton?.onClick(0.0, 0.0)
                    ?: log.error("Error: could not find create world button")

                return@register
            }

            if (screen is ConfirmScreen && state == ScreenState.ConfirmExperimentalFeatures) {
                // If there is a prompt to confirm the experimental datapack usage,
                // click yes automatically
                log.warn("Bypassing experimental warnings for a BINGO world")

                val confirmButton = Screens.getButtons(screen)
                    .find {
                        val content = it.message.content as? TranslatableTextContent
                        content?.key == "gui.yes"
                    }

                confirmButton?.onClick(0.0, 0.0)
                    ?: log.error("Error: could not find the experimental world features confirm button")
            }
        }
    }
}