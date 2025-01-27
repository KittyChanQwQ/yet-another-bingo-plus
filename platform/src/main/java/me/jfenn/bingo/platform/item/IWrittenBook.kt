package me.jfenn.bingo.platform.item

import net.minecraft.text.Text

interface IWrittenBook : IItemStack {
    var title: String?
    var author: String?
    fun setPages(pages: List<Text>)
}