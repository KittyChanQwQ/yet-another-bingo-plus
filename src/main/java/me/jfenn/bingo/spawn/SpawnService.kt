package me.jfenn.bingo.spawn

import me.jfenn.bingo.card.BingoTeam
import me.jfenn.bingo.config.BingoConfig
import me.jfenn.bingo.minecraftServer
import net.minecraft.block.Blocks
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.mob.MobEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
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

        teamSpawnpoints.forEach { (_, blockPos) ->
            createTeamChest(config, blockPos)
        }
    }

    /**
     * Place a team kit chest in the world, nearby the provided [spawnpoint]
     * Similar behavior to the BonusChestFeature
     */
    private fun createTeamChest(config: BingoConfig, spawnpoint: BlockPos) {
        if (!config.isTeamKit) return

        // TODO: world might be configurable in the future
        val world = minecraftServer.overworld

        val chunk = world.getChunk(spawnpoint)
        val heightmap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES)

        // Choose a random position in the chunk to spawn the team chest at
        val chestBlockPos = buildList {
            for (x in 0..16) {
                for (z in 0..16) {
                    add(Pair(x, z))
                }
            }
        }
            .shuffled()
            .asSequence()
            .map { (x, z) ->
                val y = heightmap.get(x, z)
                chunk.pos.getBlockPos(x, y, z)
            }
            .find {  blockPos ->
                world.isAir(blockPos) || world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty
            }

        if (chestBlockPos == null) {
            log.error("No valid locations to create a team kit chest!")
            return
        }

        // Create the team chest block/entity at the chosen position
        world.setBlockState(chestBlockPos, Blocks.CHEST.defaultState)
        val entity = world.getBlockEntity(chestBlockPos) as? ChestBlockEntity ?: run {
            log.error("Creating a team kit chest did not create a block entity. This should never happen!!")
            return
        }

        // Place torches around the chest in each direction
        val torchState = Blocks.TORCH.defaultState
        Direction.Type.HORIZONTAL
            .map { direction -> chestBlockPos.offset(direction) }
            .filter { blockPos -> torchState.canPlaceAt(world, blockPos) }
            .forEach { blockPos -> world.setBlockState(blockPos, torchState) }

        // Fill the chest with the configured item stacks
        config.teamKitItems
            .flatMap { it.toItemStacks() }
            .forEachIndexed { i, itemStack ->
                entity.setStack(i, itemStack)
            }
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