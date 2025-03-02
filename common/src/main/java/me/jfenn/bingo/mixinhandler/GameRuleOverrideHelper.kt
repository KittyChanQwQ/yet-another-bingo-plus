package me.jfenn.bingo.mixinhandler

import me.jfenn.bingo.platform.world.IGameRule
import java.util.concurrent.ConcurrentHashMap

object GameRuleOverrideHelper {
    val gameRuleOverrides = ConcurrentHashMap<String, Any>()

    internal fun <T: Any> setOverride(gameRule: IGameRule<T>, value: T?) {
        if (value != null) {
            gameRuleOverrides[gameRule.name] = value
        } else {
            gameRuleOverrides.remove(gameRule.name)
        }
    }
}