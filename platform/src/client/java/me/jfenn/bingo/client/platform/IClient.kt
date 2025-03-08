package me.jfenn.bingo.client.platform

import me.jfenn.bingo.client.platform.renderer.IFont

interface IClient {
    val font: IFont
    val player: IClientPlayer
    val isInSingleplayer: Boolean
}
