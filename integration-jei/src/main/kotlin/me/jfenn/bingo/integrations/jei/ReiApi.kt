package me.jfenn.bingo.integrations.jei

import me.jfenn.bingo.client.integrations.jei.IJeiApi
import me.jfenn.bingo.platform.item.IItemStack
import me.shedaniel.rei.api.client.view.ViewSearchBuilder
import me.shedaniel.rei.api.common.entry.EntryStack
import me.shedaniel.rei.api.common.util.EntryStacks

class ReiApi : IJeiApi {
    override fun openItemRecipe(stack: IItemStack): Boolean {
        val entryStacksOf = EntryStacks::class.java.getMethod("of", stack.stack.javaClass)
        val entryStacks = entryStacksOf.invoke(null, stack.stack) as EntryStack<*>
        return ViewSearchBuilder.builder()
            .addRecipesFor(entryStacks)
            .open()
    }
}