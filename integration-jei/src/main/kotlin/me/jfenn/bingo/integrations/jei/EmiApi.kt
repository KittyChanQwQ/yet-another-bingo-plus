package me.jfenn.bingo.integrations.jei

import dev.emi.emi.api.EmiApi
import dev.emi.emi.api.stack.EmiStack
import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.platform.item.IItemStack

class EmiApi : IJeiApi {
    override fun openItemRecipe(stack: IItemStack): Boolean {
        val emiStackOf = EmiStack::class.java.getMethod("of", stack.stack.javaClass)
        val emiStack = emiStackOf.invoke(null, emiStackOf) as EmiStack
        EmiApi.displayRecipes(emiStack)
        return true
    }
}