package me.jfenn.bingo.client.common.world

import me.jfenn.bingo.client.api.IButtonService
import me.jfenn.bingo.client.api.IWorldService
import me.jfenn.bingo.common.scope.BingoComponent
import me.jfenn.bingo.common.scope.ScopeManager
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.state.GameState
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.server.MinecraftServer
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class BingoWorldManager(
    private val buttonService: IButtonService,
    private val worldService: IWorldService,
) : BingoComponent() {

    init {
        ScopeManager.onCleanup { scope ->
            val state = scope.get<BingoState>()
            val server = scope.get<MinecraftServer>()

            if (state.state != GameState.PLAYING && worldService.isBingoWorld(server)) {
                // If the game is not in progress, delete it!
                worldService.deleteSave(server)
            }
        }

        ScreenEvents.AFTER_INIT.register { _, screen, _, height ->
            if (screen is TitleScreen) {
                val buttons = Screens.getButtons(screen)
                buttons.add(
                    buttonService.createButton(
                        x = 10,
                        y = height - 70,
                        width = 80,
                        height = 50,
                        texture = Identifier.of("minecraft", "bingo/button"),
                        focusedTexture = Identifier.of("minecraft", "bingo/button_focused"),
                        onClick = { worldService.createBingoWorld() },
                        message = Text.literal("BINGO!"),
                    )
                )
            }
        }
    }
}