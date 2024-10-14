package me.jfenn.bingo.client.api

interface IOptionsAccessor {

    fun isDebugEnabled(): Boolean

    fun isPlayerListPressed(): Boolean

    fun isSneakPressed(): Boolean

    fun isHudHidden(): Boolean

}
