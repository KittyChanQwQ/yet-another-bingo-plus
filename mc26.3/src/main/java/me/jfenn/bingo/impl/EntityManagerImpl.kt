package me.jfenn.bingo.impl

import me.jfenn.bingo.common.utils.toNbt
import me.jfenn.bingo.impl.inventory.InventoryImpl
import me.jfenn.bingo.platform.*
import me.jfenn.bingo.platform.EntityType
import me.jfenn.bingo.platform.inventory.IInventory
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.ITextFactory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.entity.vehicle.boat.ChestBoat
import net.minecraft.world.entity.vehicle.minecart.MinecartChest
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import org.joml.Matrix4f
import org.joml.Vector3d
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KProperty

class EntityManagerImpl(
    private val itemStackFactory: IItemStackFactory,
    private val textFactory: ITextFactory,
) : IEntityManager {
    private fun EntityType<*>.toMinecraftType(): net.minecraft.world.entity.EntityType<*> {
        return when (this) {
            EntityType.INTERACTION -> EntityTypes.INTERACTION
            EntityType.TEXT_DISPLAY -> EntityTypes.TEXT_DISPLAY
            EntityType.BLOCK_DISPLAY -> EntityTypes.BLOCK_DISPLAY
            EntityType.ARMOR_STAND -> EntityTypes.ARMOR_STAND
            EntityType.TNT -> EntityTypes.TNT
            EntityType.BAT -> EntityTypes.BAT
            EntityType.CHEST_MINECART -> EntityTypes.CHEST_MINECART
            EntityType.CHEST_BOAT -> EntityTypes.OAK_CHEST_BOAT
            else -> error("Entity type not recognized!")
        }
    }

    private fun forEntity(entity: Entity): IEntity {
        val impl = when (entity) {
            is Interaction -> InteractionEntityImpl(entity)
            is Display.TextDisplay -> TextDisplayEntityImpl(entity, textFactory)
            is Display.BlockDisplay -> BlockDisplayEntityImpl(entity)
            is ArmorStand -> ArmorStandEntityImpl(entity)
            is PrimedTnt -> TntEntityImpl(entity)
            is Bat -> BatEntityImpl(entity)
            is MinecartChest -> ChestMinecartEntityImpl(itemStackFactory, entity)
            is ChestBoat -> ChestBoatEntityImpl(itemStackFactory, entity)
            is LivingEntity -> LivingEntityImpl(entity)
            else -> EntityImpl(entity)
        }
        return impl
    }

    override fun <T : IEntity> createEntity(type: EntityType<T>, world: IServerWorld): T {
        require(world is ServerWorldImpl)
        val mcType = type.toMinecraftType()
        val entity = mcType.create(world.world, EntitySpawnReason.STRUCTURE)!!
        val impl = forEntity(entity)
        return (@Suppress("UNCHECKED_CAST") (impl as T))
    }

    override fun getEntity(world: IServerWorld, uuid: UUID): IEntity? {
        require(world is ServerWorldImpl)
        val entity = world.world.getEntity(uuid) ?: return null
        return forEntity(entity)
    }

    override fun spawnEntity(world: IServerWorld, entity: IEntity): Boolean {
        require(world is ServerWorldImpl)
        return world.world.addFreshEntity(entity.entity)
    }

    override fun iterateEntities(world: IServerWorld): Sequence<IEntity> {
        require(world is ServerWorldImpl)
        return world.world.allEntities.asSequence<Entity?>()
            .filterNotNull()
            .map { forEntity(it) }
    }
}

private class NbtBooleanDelegate(private val name: String) {
    operator fun getValue(thisRef: EntityImpl, property: KProperty<*>): Boolean = thisRef.nbt.getBooleanOr(name, false)
    operator fun setValue(thisRef: EntityImpl, property: KProperty<*>, value: Boolean) {
        return thisRef.patchNbt { putBoolean(name, value) }
    }
}

private class NbtIntDelegate(private val name: String) {
    operator fun getValue(thisRef: EntityImpl, property: KProperty<*>): Int = thisRef.nbt.getIntOr(name, 0)
    operator fun setValue(thisRef: EntityImpl, property: KProperty<*>, value: Int) {
        return thisRef.patchNbt { putInt(name, value) }
    }
}

private class NbtFloatDelegate(private val name: String) {
    operator fun getValue(thisRef: EntityImpl, property: KProperty<*>): Float = thisRef.nbt.getFloatOr(name, 0f)
    operator fun setValue(thisRef: EntityImpl, property: KProperty<*>, value: Float) {
        return thisRef.patchNbt { putFloat(name, value) }
    }
}

open class EntityImpl(
    override val entity: Entity
) : IEntity {
    override var uuid: UUID
        get() = entity.uuid
        set(value) {
            entity.uuid = value
        }

    override var pos: Vector3d
        get() = entity.position().let { Vector3d(it.x, it.y, it.z) }
        set(value) {
            entity.setPos(value.x, value.y, value.z)
        }

    override var commandTags: Set<String>
        get() = entity.entityTags()
        set(value) {
            entity.entityTags().forEach { entity.removeTag(it) }
            value.forEach { entity.addTag(it) }
        }

    override var pitch: Float
        get() = entity.xRot
        set(value) {
            entity.xRot = value
        }

    override var yaw: Float
        get() = entity.yRot
        set(value) {
            entity.yRot = value
        }

    override val name: IText
        get() = TextImpl(entity.name.copy())

    override fun discard() {
        entity.discard()
    }

    internal var nbt
        get() = TagValueOutput.createWithContext(BingoErrorReporter.getInstance(), entity.registryAccess())
            .also { entity.saveWithoutId(it) }
            .buildResult()
        set(value) {
            entity.load(TagValueInput.create(BingoErrorReporter.getInstance(), entity.registryAccess(), value))
        }

    internal fun <R> patchNbt(patch: CompoundTag.() -> R): R {
        val compound = nbt
        val ret = patch(compound)
        nbt = compound
        return ret
    }
}

class InteractionEntityImpl(
    override val entity: Interaction
) : IInteractionEntity, EntityImpl(entity) {
    override val type: EntityType<IInteractionEntity>
        get() = EntityType.INTERACTION
    override var width: Float by NbtFloatDelegate("width")
    override var height: Float by NbtFloatDelegate("height")
}

open class DisplayEntityImpl(
    override val entity: Display
) : IDisplayEntity, EntityImpl(entity) {
    override var transformation: Matrix4f
        get() = Matrix4f()
        set(value) {
            patchNbt { put("transformation", value.toNbt()) }
        }

    override var brightness: IDisplayEntity.Brightness?
        get() = nbt
            .takeIf { it.contains("brightness") }
            ?.getCompound("brightness")
            ?.getOrNull()
            ?.let {
                IDisplayEntity.Brightness(
                    block = it.getIntOr("block", 0),
                    sky = it.getIntOr("sky", 0),
                )
            }
        set(value) {
            patchNbt {
                if (value != null) {
                    put("brightness", CompoundTag().apply {
                        putInt("block", value.block)
                        putInt("sky", value.sky)
                    })
                } else {
                    remove("brightness")
                }
            }
        }
}

class TextDisplayEntityImpl(
    override val entity: Display.TextDisplay,
    private val textFactory: ITextFactory,
) : ITextDisplayEntity, DisplayEntityImpl(entity) {
    override val type: EntityType<ITextDisplayEntity>
        get() = EntityType.TEXT_DISPLAY

    override var value: IText
        get() = textFactory.from(entity.text)
        set(value) {
            entity.text = value.value
        }

    override var lineWidth: Int by NbtIntDelegate("line_width")

    override var billboard: ITextDisplayEntity.Billboard
        get() = nbt.getString("billboard")
            .getOrNull()
            ?.uppercase()
            ?.let { ITextDisplayEntity.Billboard.valueOf(it) }
            ?: ITextDisplayEntity.Billboard.FIXED
        set(value) {
            patchNbt { putString("billboard", value.name.lowercase()) }
        }

    override var alignment: ITextDisplayEntity.TextAlignment
        get() = nbt.getString("alignment")
            .getOrNull()
            ?.uppercase()
            ?.let { ITextDisplayEntity.TextAlignment.valueOf(it) }
            ?: ITextDisplayEntity.TextAlignment.CENTER
        set(value) {
            patchNbt { putString("alignment", value.name.lowercase()) }
        }

    override var background: Int by NbtIntDelegate("background")
    override var shadow: Boolean by NbtBooleanDelegate("shadow")
}

class BlockDisplayEntityImpl(
    override val entity: Display.BlockDisplay,
) : IBlockDisplayEntity, DisplayEntityImpl(entity) {
    override val type: EntityType<IBlockDisplayEntity>
        get() = EntityType.BLOCK_DISPLAY

    override var blockIdentifier: String
        get() = BuiltInRegistries.BLOCK.getKey(entity.blockState.block).toString()
        set(value) {
            val block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(value))
            entity.blockState = block.defaultBlockState()
        }
}

class ArmorStandEntityImpl(
    override val entity: ArmorStand,
) : IArmorStandEntity, EntityImpl(entity) {
    override fun equipStack(slot: IArmorStandEntity.EquipmentSlot, stack: IItemStack) {
        val mcSlot = when (slot) {
            IArmorStandEntity.EquipmentSlot.MAINHAND -> EquipmentSlot.MAINHAND
            IArmorStandEntity.EquipmentSlot.OFFHAND -> EquipmentSlot.OFFHAND
            IArmorStandEntity.EquipmentSlot.HEAD -> EquipmentSlot.HEAD
            IArmorStandEntity.EquipmentSlot.CHEST -> EquipmentSlot.CHEST
            IArmorStandEntity.EquipmentSlot.LEGS -> EquipmentSlot.LEGS
            IArmorStandEntity.EquipmentSlot.FEET -> EquipmentSlot.FEET
        }
        entity.setItemSlot(mcSlot, stack.stack)
    }
}

class TntEntityImpl(
    override val entity: PrimedTnt,
) : ITntEntity, EntityImpl(entity) {
    override var fuse: Int by entity::fuse
}

open class LivingEntityImpl(
    final override val entity: LivingEntity
) : ILivingEntity, EntityImpl(entity) {
    override var invulnerable: Boolean by NbtBooleanDelegate("Invulnerable")
    override var noGravity: Boolean by NbtBooleanDelegate("NoGravity")
    override var silent: Boolean by NbtBooleanDelegate("Silent")
    override var noAI: Boolean by NbtBooleanDelegate("NoAI")
    override var persistenceRequired: Boolean by NbtBooleanDelegate("PersistenceRequired")

    override var health: Float by entity::health
    override val maxHealth: Float by entity::maxHealth

    override var air: Int by entity::airSupply
    override val maxAir: Int by entity::maxAirSupply

    override fun addEffect(
        type: EffectType,
        duration: Int,
        amplifier: Int,
        ambient: Boolean,
        visible: Boolean
    ): IStatusEffectHandle {
        val instance = MobEffectInstance(
            when (type) {
                EffectType.NIGHT_VISION -> MobEffects.NIGHT_VISION
                EffectType.SLOWNESS -> MobEffects.SLOWNESS
                EffectType.JUMP_BOOST -> MobEffects.JUMP_BOOST
                EffectType.INVISIBILITY -> MobEffects.INVISIBILITY
                EffectType.OTHER -> throw IllegalArgumentException("[StatusEffectsImpl] OTHER is not a valid effect type!")
            },
            duration,
            amplifier,
            ambient,
            visible
        )
        entity.addEffect(instance)
        return StatusEffectHandle(instance)
    }

    override fun getEffects(): List<IStatusEffectHandle> {
        return entity.activeEffects.map { StatusEffectHandle(it) }
    }

    override fun removeEffect(effect: IStatusEffectHandle) {
        require(effect is StatusEffectHandle)
        entity.removeEffect(effect.instance.effect)
    }
}

class BatEntityImpl(
    entity: Bat
) : IBatEntity, LivingEntityImpl(entity)

class ChestMinecartEntityImpl(
    private val itemStackFactory: IItemStackFactory,
    override val entity: MinecartChest,
) : IChestMinecartEntity, EntityImpl(entity) {
    override val inventory: IInventory
        get() = InventoryImpl(itemStackFactory, entity)
}

class ChestBoatEntityImpl(
    private val itemStackFactory: IItemStackFactory,
    override val entity: ChestBoat,
) : IChestBoatEntity, EntityImpl(entity) {
    override val inventory: IInventory
        get() = InventoryImpl(itemStackFactory, entity)
}
