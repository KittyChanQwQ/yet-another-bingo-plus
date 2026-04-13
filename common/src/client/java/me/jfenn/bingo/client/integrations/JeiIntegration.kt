package me.jfenn.bingo.client.integrations

import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.platform.item.IItemStack

internal class JeiIntegration(
    private val integrations: List<IJeiApi>,
) : IJeiApi {

    override fun openItemRecipe(stack: IItemStack): Boolean {
        return integrations.find { it.openItemRecipe(stack) } != null
    }
}