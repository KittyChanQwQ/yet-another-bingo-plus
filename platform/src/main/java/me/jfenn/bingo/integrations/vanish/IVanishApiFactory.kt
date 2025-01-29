package me.jfenn.bingo.integrations.vanish

import me.jfenn.bingo.platform.IModEnvironment

interface IVanishApiFactory {
    fun create(environment: IModEnvironment): IVanishApi
}