package me.jfenn.bingo.api

import me.jfenn.bingo.api.card.ICard
import me.jfenn.bingo.api.card.ICardService
import me.jfenn.bingo.common.card.CardService
import me.jfenn.bingo.common.state.BingoState
import me.jfenn.bingo.common.team.BingoTeamKey

internal class CardServiceImpl(
    private val state: BingoState,
    private val cardService: CardService,
) : ICardService {
    override fun getActiveCard(): ICard = state.getActiveCard()

    override fun getTeamCard(teamName: String): ICard? =
        state.teams[BingoTeamKey(teamName)]
            ?.let { state.getCard(it) }

    override fun shuffleCard(card: ICard) {
        val card = requireNotNull(state.getCard(card.id))
        cardService.shuffleCard(card)
    }

    override fun replaceEntry(
        card: ICard,
        x: Int,
        y: Int,
        objectiveId: String
    ) {
        val card = requireNotNull(state.getCard(card.id))
        cardService.replaceEntry(card, x, y, objectiveId)
    }
}