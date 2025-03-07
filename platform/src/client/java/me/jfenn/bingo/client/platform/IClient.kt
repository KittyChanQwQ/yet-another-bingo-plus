package me.jfenn.bingo.client.platform

interface IClient {
    val player: IClientPlayer
    val isInSingleplayer: Boolean
}
