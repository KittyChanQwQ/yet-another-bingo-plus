package me.jfenn.bingo.api.card

interface ICardEntry {
    val objectiveId: String
    val objectiveTier: ObjectiveTier?
}