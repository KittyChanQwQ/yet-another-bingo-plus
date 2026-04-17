package me.jfenn.bingo.api.card

interface ICardService {
    /**
     * Always returns the first active card. This should only be used in the pre-game/lobby state,
     * when you need to know the visible preview card but there are no teams declared yet.
     */
    fun getActiveCard(): ICard

    fun getTeamCard(teamName: String): ICard?

    fun shuffleCard(card: ICard)

    fun replaceEntry(card: ICard, x: Int, y: Int, objectiveId: String)

}