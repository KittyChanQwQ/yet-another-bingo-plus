package me.jfenn.bingo.api.card

import java.util.UUID

interface ICard {
    val id: UUID
    val seed: Long
    fun objective(x: Int, y: Int): ICardEntryAccess?
}
