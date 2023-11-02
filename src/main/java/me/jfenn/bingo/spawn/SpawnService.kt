package me.jfenn.bingo.spawn

import me.jfenn.bingo.BingoGame
import me.jfenn.bingo.NBT_BINGO_IGNORE
import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.minecraftServer
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.gen.feature.BonusChestFeature
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

    private fun createItemStacks(spawnItem: BingoConfig.SpawnItem): List<ItemStack> {
        val identifier = Identifier.tryParse(spawnItem.id) ?: run {
            log.error("Cannot parse spawnEquipmentItems id '${spawnItem.id}'")
            null
        } ?: return emptyList()

        val item = try {
            Registries.ITEM.get(identifier)
        } catch (e: Error) {
            log.error("spawnEquipmentItems id '${spawnItem.id}' is not in the item registry", e)
            null
        } ?: return emptyList()

        val fullStackCount = spawnItem.count / item.maxCount
        val extraStackCount = spawnItem.count % item.maxCount

        val itemStacks = sequence {
            repeat(fullStackCount) {
                yield(ItemStack(item, item.maxCount))
            }

            if (extraStackCount > 0) {
                yield(ItemStack(item, extraStackCount))
            }
        }

        return itemStacks
            .map {
                try {
                    it.nbt = StringNbtReader.parse(spawnItem.nbt)
                } catch (e: Error) {
                    log.error("spawnEquipmentItems nbt '${spawnItem.nbt}' could not be parsed", e)
                }

                // set BINGO_IGNORE flag so that spawn items do not count on the bingo card
                it.getOrCreateNbt().apply {
                    putBoolean(NBT_BINGO_IGNORE, true)
                }

                it
            }
            .toList()
    }

    /**
     * Equip the player with any configured spawn equipment
     */
    fun giveSpawnEquipment(config: BingoConfig, players: List<ServerPlayerEntity>) {
        if (!config.isSpawnEquipment) return

        val itemStacks = config.spawnEquipmentItems
            .flatMap { createItemStacks(it) }

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