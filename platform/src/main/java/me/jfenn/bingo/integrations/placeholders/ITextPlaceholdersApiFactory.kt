package me.jfenn.bingo.integrations.placeholders

import me.jfenn.bingo.platform.IModEnvironment
import me.jfenn.bingo.platform.text.ITextFactory

interface ITextPlaceholdersApiFactory {
    fun create(environment: IModEnvironment, textFactory: ITextFactory): ITextPlaceholdersApi
}