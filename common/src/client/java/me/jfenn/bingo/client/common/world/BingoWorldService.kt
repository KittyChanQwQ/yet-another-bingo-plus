package me.jfenn.bingo.client.common.world

import me.jfenn.bingo.common.text.TextProvider
import me.jfenn.bingo.generated.StringKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.MessageScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen

class BingoWorldService(
    private val worldState: BingoWorldState,
    private val text: TextProvider,
) {

    val client = MinecraftClient.getInstance()

    fun disconnect(
        loadingScreen: Screen = MessageScreen(text.string(StringKey.WorldPreparing)),
    ) {
        client.world?.disconnect()
        client.disconnect(loadingScreen)
        client.setScreen(TitleScreen())
    }

    fun reopenClientWorld() {
        worldState.state = ScreenState.ImmediatelyReopenBingoWorld
        disconnect()
    }

}