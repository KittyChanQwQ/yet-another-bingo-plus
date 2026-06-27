package me.jfenn.bingo.platform.world

interface IGameRules {
    val announceAdvancements: IGameRule<Boolean>
    val showDeathMessages: IGameRule<Boolean>
    val keepInventory: IGameRule<Boolean>
    val pvp: IGameRule<Boolean>
    val randomTickSpeed: IGameRule<Int>
    val fireSpreadRange: IGameRule<Int>

    fun get(name: String): IGameRule<*>?
}

interface IGameRule<T> {
    val name: String
    var value: T
}
