package me.jfenn.bingo.client.api

import java.util.*

interface ISessionAccessor {
    fun getPlayerUuid(): UUID?
}