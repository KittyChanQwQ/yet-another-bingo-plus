package me.jfenn.bingo.client

import com.google.common.collect.ImmutableList
import me.jfenn.bingo.common.BINGO_WORLD_PREFIX
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.resource.DataConfiguration
import net.minecraft.resource.DataPackSettings
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

object Main : ClientModInitializer {

    private val log = LoggerFactory.getLogger(this::class.java);

    fun copyDataPack(toPath: Path) {
        toPath.toFile().mkdirs()

        val datapackPath = toPath.resolve("bingo-lobby.zip")
        Files.deleteIfExists(datapackPath)
        Files.createFile(datapackPath)

        this::class.java.getResourceAsStream("/bingo/datapack.zip").use { input ->
            FileOutputStream(datapackPath.toFile()).use { output ->
                IOUtils.copy(input, output)
            }
        }
    }

    fun createBingoWorld(client: MinecraftClient, parent: Screen) {
        CreateWorldScreen.create(client, parent)
    }

    private var isCreatingBingoWorld = false
    // this flag prevents infinite recursion, as there is a setScreen call in applyDataPacks
    private var isApplyingLobbyDataPack = false

    private var isOpeningBingoWorld = false

    override fun onInitializeClient() {
        ScreenEvents.BEFORE_INIT.register { client, screen, width, height ->
            if (screen is CreateWorldScreen && !isApplyingLobbyDataPack) {
                // Add a prefix to the names of bingo worlds
                if (isCreatingBingoWorld) {
                    screen.worldCreator.worldName = "$BINGO_WORLD_PREFIX ${screen.worldCreator.worldName}"
                }
            }
        }

        ScreenEvents.AFTER_INIT.register { client, screen, width, height ->
            if (screen is TitleScreen) {
                val buttons = Screens.getButtons(screen)
                buttons.add(
                    BingoButtonWidget(
                        10,
                        height - 80,
                        60,
                        60,
                        Text.literal("BINGO!")
                    ) {
                        isCreatingBingoWorld = true
                        createBingoWorld(client, screen)
                        isCreatingBingoWorld = false
                    }
                )
            }

            if (screen is CreateWorldScreen && !isApplyingLobbyDataPack) {
                if (isOpeningBingoWorld) {
                    // Once the world creation screen is reached, immediately press the
                    // create world button
                    isOpeningBingoWorld = false

                    val createWorldButton = Screens.getButtons(screen)
                        .find {
                            val content = it.message.content as? TranslatableTextContent
                            content?.key == "selectWorld.create"
                        }

                    createWorldButton?.onClick(0.0, 0.0)
                        ?: log.error("Error: could not find create world button")

                    return@register
                }

                // Copy the built-in lobby data pack to the world creator's temp dir
                // - we want this to happen on every world creation, so that the pack is available
                //   even if the user has not specifically pressed the "BINGO!" button.
                screen.dataPackTempDir?.let {
                    copyDataPack(it)
                }

                val pair = screen.getScannedPack(screen.worldCreator.generatorOptionsHolder.dataConfiguration)
                    ?: return@register

                // this flag prevents infinite recursion, as there is a setScreen call in applyDataPacks
                isApplyingLobbyDataPack = true
                screen.applyDataPacks(pair.second, false) {}
                isApplyingLobbyDataPack = false

                // actually enable the bingo lobby datapack
                if (isCreatingBingoWorld) {
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

                    isOpeningBingoWorld = true
                    screen.validateDataPacks(pair.second, dataConfiguration) {}
                }
            }
        }
    }
}
