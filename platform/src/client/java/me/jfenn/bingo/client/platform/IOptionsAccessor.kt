package me.jfenn.bingo.client.platform

interface IOptionsAccessor {

    val chatScale: Double

    fun isDebugEnabled(): Boolean

    fun isPlayerListPressed(): Boolean

    fun isSneakPressed(): Boolean

    fun isHudHidden(): Boolean

}
