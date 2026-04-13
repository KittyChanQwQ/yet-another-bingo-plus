package me.jfenn.bingo.platform.item

import me.jfenn.bingo.platform.IPlayerHandle

interface IPlayerHead : IItemStack {
    fun setSkullOwner(player: IPlayerHandle)
}