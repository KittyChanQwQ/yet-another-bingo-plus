package me.jfenn.bingo.impl

import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IStatHandle
import me.jfenn.bingo.api.IStatManager
import net.minecraft.registry.Registries
import net.minecraft.stat.StatHandler
import net.minecraft.stat.StatType
import net.minecraft.util.Identifier

class StatManagerImpl : IStatManager {

    override fun getById(type: String, name: String?): IStatHandle? {
        val typeId = Identifier.tryParse(type) ?: return null
        val statType = Registries.STAT_TYPE.get(typeId) ?: return null

        if (name != null) {
            val nameId = Identifier.tryParse(name) ?: return null
            val statName = statType.registry.get(nameId) ?: return null

            return StatHandleImpl(
                type = @Suppress("UNCHECKED_CAST") (statType as StatType<Any>),
                name = statName,
            )
        } else {
            return SummedStatHandleImpl(statType)
        }
    }

    inner class StatHandleImpl<T>(
        private val type: StatType<T>,
        private val name: T,
    ) : IStatHandle {
        override fun getForPlayer(player: IPlayerHandle): Int {
            require(player is PlayerHandle)
            val statHandler: StatHandler = player.player.statHandler
            return statHandler.getStat(type, name)
        }
    }

    inner class SummedStatHandleImpl<T>(
        private val type: StatType<T>,
    ) : IStatHandle {
        override fun getForPlayer(player: IPlayerHandle): Int {
            require(player is PlayerHandle)
            val statHandler: StatHandler = player.player.statHandler

            var count = 0
            for (stat in type.registry)
                count += statHandler.getStat(type, stat)
            return count
        }
    }
}