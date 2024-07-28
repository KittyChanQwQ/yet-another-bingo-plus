package me.jfenn.bingo.api

interface ITagAccessor {

    fun getItemTag(id: String): List<String>?

}