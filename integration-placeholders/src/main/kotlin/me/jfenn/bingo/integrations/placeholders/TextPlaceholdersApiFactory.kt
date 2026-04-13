package me.jfenn.bingo.integrations.placeholders

import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.text.ITextFactory

class TextPlaceholdersApiFactory : ITextPlaceholdersApiFactory {
    override fun create(environment: IModEnvironment, textFactory: ITextFactory): ITextPlaceholdersApi {
        return when {
            environment.isModLoaded("placeholder-api") -> TextPlaceholdersApi(textFactory)
            else -> DummyPlaceholders
        }
    }
}