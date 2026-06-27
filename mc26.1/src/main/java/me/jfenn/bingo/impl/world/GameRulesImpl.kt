package me.jfenn.bingo.impl.world

import me.jfenn.bingo.platform.world.IGameRule
import me.jfenn.bingo.platform.world.IGameRules
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleType
import net.minecraft.world.level.gamerules.GameRules

class GameRulesImpl(
    private val server: MinecraftServer,
) : IGameRules {

    override val announceAdvancements = BooleanRule(GameRules.SHOW_ADVANCEMENT_MESSAGES)
    override val showDeathMessages = BooleanRule(GameRules.SHOW_DEATH_MESSAGES)
    override val keepInventory = BooleanRule(GameRules.KEEP_INVENTORY)
    override val pvp = BooleanRule(GameRules.PVP)
    override val randomTickSpeed = IntRule(GameRules.RANDOM_TICK_SPEED)
    override val fireSpreadRange = IntRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER)

    override fun get(name: String): IGameRule<*>? {
        val rule = BuiltInRegistries.GAME_RULE.getValue(Identifier.withDefaultNamespace(name))
            ?: return null

        return when (rule.gameRuleType()) {
            GameRuleType.BOOL -> BooleanRule(
                @Suppress("UNCHECKED_CAST") (rule as GameRule<Boolean>)
            )
            GameRuleType.INT -> IntRule(
                @Suppress("UNCHECKED_CAST") (rule as GameRule<Int>)
            )
        }
    }

    inner class BooleanRule(
        private val rule: GameRule<Boolean>,
    ) : IGameRule<Boolean> {
        override val name: String get() = rule.identifier.path
        override var value: Boolean
            get() = server.overworld().gameRules.get(rule)
            set(value) {
                server.allLevels.forEach {
                    it.gameRules.set(rule, value, server)
                }
            }
    }

    inner class IntRule(
        private val rule: GameRule<Int>,
    ) : IGameRule<Int> {
        override val name: String get() = rule.identifier.path
        override var value: Int
            get() = server.overworld().gameRules.get(rule)
            set(value) {
                server.allLevels.forEach {
                    it.gameRules.set(rule, value, server)
                }
            }
    }
}