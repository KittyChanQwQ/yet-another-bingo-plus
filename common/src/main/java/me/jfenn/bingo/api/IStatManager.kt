package me.jfenn.bingo.api

interface IStatManager {
    fun getById(type: String, name: String?): IStatHandle?
}

interface IStatHandle {
    fun getForPlayer(player: IPlayerHandle): Int
}
