package me.jfenn.bingo.integrations.vanish

import me.jfenn.bingo.platform.IModEnvironment

class VanishApiFactory : IVanishApiFactory {
    override fun create(environment: IModEnvironment): IVanishApi {
        return when {
            environment.isModLoaded("vanish") -> VanishApiImpl()
            else -> DummyVanish
        }
    }
}