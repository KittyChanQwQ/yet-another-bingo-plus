package me.jfenn.bingo.platform

interface IStatManager {
    fun getById(type: String, name: String?): IStatHandle?
}

interface IStatHandle {
    fun getForPlayer(player: IPlayerHandle): Int
}
