package me.jfenn.bingo.client.common.world

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.MessageScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.text.Text

class BingoWorldService(
    private val worldState: BingoWorldState,
) {

    val client = MinecraftClient.getInstance()

    fun disconnect(
        loadingScreen: Screen = MessageScreen(Text.translatable("bingo.world.preparing")),
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