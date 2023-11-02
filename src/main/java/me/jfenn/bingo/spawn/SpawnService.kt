package me.jfenn.bingo.spawn

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import org.slf4j.LoggerFactory

object SpawnService {

    private val log = LoggerFactory.getLogger(this::class.java)

    var teamSpawnpoints = mapOf<BingoTeam, BlockPos>()

    /**
     * Create team spawnpoints and teleport teams to their spawns
     */
    fun createSpawnpoints(config: BingoConfig) {
        // spread player teams to spawnpoint
        val spread = SpreadPlayers(world = minecraftServer.overworld)
        teamSpawnpoints = spread.spread(config.spreadPlayersDistance)
        spread.teleportPlayers(teamSpawnpoints)
    }

    /**
     * Equip the player with any configured spawn equipment
     */
    fun giveSpawnEquipment(config: BingoConfig, players: List<ServerPlayerEntity>) {
        if (!config.isPlayerKit) return

        val itemStacks = config.playerKitItems
            .flatMap { it.toItemStacks() }

        for (stack in itemStacks) {
            for (player in players) {
                // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
                val slot = MobEntity.getPreferredEquipmentSlot(stack)
                if (player.canEquip(stack) && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                    player.equipStack(slot, stack)
                } else {
                    // otherwise, put it in the player's inventory
                    player.giveItemStack(stack)
                }
            }
        }
    }

}