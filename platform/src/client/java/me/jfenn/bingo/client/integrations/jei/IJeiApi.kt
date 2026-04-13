package me.jfenn.bingo.client.integrations.jei

import me.jfenn.bingo.platform.item.IItemStack

interface IJeiApi {
    fun openItemRecipe(stack: IItemStack): Boolean
}