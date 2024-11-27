package me.jfenn.bingo.platform

interface ITagAccessor {

    fun getItemTag(id: String): List<String>?

}