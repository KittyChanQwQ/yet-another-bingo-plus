package me.jfenn.bingo.api.item

import java.util.*

interface IPlayerHead : IItemStack {
    fun setSkullOwner(playerId: UUID, playerName: String)
}