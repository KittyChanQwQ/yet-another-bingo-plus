package me.jfenn.bingo.impl

import com.mojang.authlib.GameProfile
import me.jfenn.bingo.api.IPlayerHandle
import me.jfenn.bingo.api.IPlayerManager
import me.jfenn.bingo.api.PlayerSoundCategory
import me.jfenn.bingo.api.PlayerSoundEvent
import me.jfenn.bingo.api.item.IItemStack
import me.jfenn.bingo.api.item.IItemStackFactory
import me.jfenn.bingo.common.team.BingoPlayerProfile
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Equipment
import net.minecraft.network.packet.c2s.common.SyncedClientOptions
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.util.*

class PlayerManager(
    private val server: MinecraftServer,
    private val itemStackFactory: IItemStackFactory,
) : IPlayerManager {
    override fun forPlayer(player: ServerPlayerEntity): IPlayerHandle {
        return PlayerHandle(player)
    }

    override fun getPlayer(uuid: UUID): IPlayerHandle? {
        return server.playerManager.getPlayer(uuid)
            ?.let { PlayerHandle(it) }
    }

    override fun getPlayers(): List<IPlayerHandle> {
        return server.playerManager.playerList
            .map { PlayerHandle(it) }
    }

    override fun getOfflinePlayer(profile: BingoPlayerProfile): IPlayerHandle {
        val player = ServerPlayerEntity(
            server,
            server.overworld,
            GameProfile(profile.uuid, profile.name),
            SyncedClientOptions.createDefault(),
        )

        server.playerManager.loadPlayerData(player)
        return PlayerHandle(player)
    }

    fun PlayerSoundEvent.toSoundEvent() = when (this) {
        PlayerSoundEvent.BLOCK_LEVER_CLICK -> SoundEvents.BLOCK_LEVER_CLICK
        PlayerSoundEvent.ENTITY_PLAYER_LEVELUP -> SoundEvents.ENTITY_PLAYER_LEVELUP
        PlayerSoundEvent.BLOCK_NOTE_BLOCK_BASS -> SoundEvents.BLOCK_NOTE_BLOCK_BASS.value()
        PlayerSoundEvent.ITEM_LODESTONE_COMPASS_LOCK -> SoundEvents.ITEM_LODESTONE_COMPASS_LOCK
        PlayerSoundEvent.BLOCK_PORTAL_TRAVEL -> SoundEvents.BLOCK_PORTAL_TRAVEL
        PlayerSoundEvent.BLOCK_NOTE_BLOCK_PLING -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()
    }

    fun PlayerSoundCategory.toSoundCategory() = when (this) {
        PlayerSoundCategory.MAIN -> SoundCategory.MASTER
        PlayerSoundCategory.RECORDS -> SoundCategory.RECORDS
        PlayerSoundCategory.BLOCKS -> SoundCategory.BLOCKS
    }

    override fun playToAround(
        player: ServerPlayerEntity?,
        sound: PlayerSoundEvent,
        category: PlayerSoundCategory,
        volume: Float,
        pitch: Float,
        position: Triple<Double, Double, Double>,
        world: ServerWorld,
    ) {
        val (x, y, z) = position
        val soundEntry = Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_LEVER_CLICK)
        val packet = PlaySoundS2CPacket(soundEntry, category.toSoundCategory(), x, y, z, volume, pitch, server.overworld.seed)
        player?.networkHandler?.sendPacket(packet)
        server.playerManager.sendToAround(player, x, y, z, soundEntry.value().getDistanceToTravel(volume).toDouble(), world.registryKey, packet)
    }

    inner class PlayerHandle(
        override val player: ServerPlayerEntity
    ) : IPlayerHandle {
        override val isAlive: Boolean
            get() = player.isAlive
        override val isSpectator: Boolean
            get() = player.isSpectator

        override val uuid: UUID
            get() = player.uuid
        override val name: String
            get() = player.nameForScoreboard

        override fun allHeldStacks(): Sequence<IItemStack> {
            return sequence {
                // yield all items in inventory
                yieldAll(allInventorySlots().map { (_, stack) -> stack })

                // yield the current cursor stack
                yield(itemStackFactory.forStack(player.currentScreenHandler.cursorStack))
            }
        }

        override fun allInventorySlots(): Sequence<Pair<Int, IItemStack>> {
            return sequence {
                // yield all items in inventory
                for (i in 0 until player.inventory.size()) {
                    yield(Pair(i, player.inventory.getStack(i)))
                }
            }.map { (i, stack) ->
                i to itemStackFactory.forStack(stack)
            }
        }

        override fun giveOrEquipStack(stack: IItemStack) {
            val itemStack = stack.stack

            // if the item can be equipped (e.g. iron chestplate), put it in an equipment slot
            val slot = Equipment.fromStack(itemStack)?.slotType
            if (slot != null && player.canEquip(itemStack) && slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                player.equipStack(slot, itemStack)
            } else {
                // otherwise, put it in the player's inventory
                player.giveItemStack(itemStack)
            }
        }

        override fun giveItemStack(stack: IItemStack) {
            player.giveItemStack(stack.stack)
        }

        override fun removeStack(slot: Int) {
            player.inventory.removeStack(slot)
        }

        override fun setStack(slot: Int, stack: IItemStack) {
            player.getStackReference(slot).set(stack.stack)
        }

        override fun playSound(sound: PlayerSoundEvent, category: PlayerSoundCategory, volume: Float, pitch: Float) {
            player.playSoundToPlayer(
                sound.toSoundEvent(),
                category.toSoundCategory(),
                volume,
                pitch,
            )
        }
    }
}