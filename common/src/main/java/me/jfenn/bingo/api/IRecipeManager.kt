package me.jfenn.bingo.api

interface IRecipeManager {
    fun listRecipes(): List<IRecipe>
    fun unlockRecipes(player: IPlayerHandle, recipes: List<IRecipe>)
}

interface IRecipe
