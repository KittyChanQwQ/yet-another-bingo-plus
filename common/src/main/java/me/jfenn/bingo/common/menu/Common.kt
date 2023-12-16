package me.jfenn.bingo.common.menu

import me.jfenn.bingo.common.state.BingoContext
import me.jfenn.bingo.common.utils.EventListener
import me.jfenn.bingo.common.utils.toNbt
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

const val MENU_HITBOX_HEIGHT = 0.25f
const val MENU_HITBOX_WIDTH = 1f

const val MENU_ITEM_OFFSET = 0.5
const val MENU_ITEM_HEIGHT = 0.3

interface Property<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}

interface MutableProperty<T>: Property<T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

class ConstantProperty<T>(val value: T): Property<T> {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}

class DelegatedProperty<T>(
    private var getter: () -> T,
    private var setter: (T) -> Unit,
): MutableProperty<T> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getter()
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setter(value)
    }

}

data class MenuEntityHandle<T: Entity>(
    val id: UUID = UUID.randomUUID(),
    val type: EntityType<T>,
    val pos: Vec3d,
    val nbt: NbtCompound.(ctx: BingoContext) -> Unit = {},
    val onUpdate: EventListener<T> = EventListener(),
)

fun component(setup: MenuComponent.() -> Unit): MenuComponent {
    val component = MenuComponent()
    component.setup()
    return component
}

open class MenuComponent {

    private var tracker: MenuInstance? = null
    private val entities: MutableList<MenuEntityHandle<*>> = mutableListOf()

    val onTick = EventListener<MenuInstance>()
    val onUpdate = EventListener<MenuInstance>()
    val onDespawn = EventListener<Unit>()

    private var isDirty = true

    fun markDirty() {
        isDirty = true
    }

    fun <T> propertyRef(ref: KMutableProperty0<T>): MutableProperty<T> {
        return DelegatedProperty(
            getter = {
                ref.get()
            },
            setter = {
                ref.set(it)
                markDirty()
            },
        )
    }

    fun <T> computedProperty(getter: () -> T): Property<T> {
        // this assumes that anything used in the getter is already observed
        // so this does not need to add any listeners
        return DelegatedProperty(getter) {}
    }

    fun <T: Entity> registerEntity(type: EntityType<T>, pos: Vec3d, nbt: NbtCompound.(BingoContext) -> Unit = {}): MenuEntityHandle<T> {
        val handle = MenuEntityHandle(
            id = UUID.randomUUID(),
            type = type,
            pos = pos,
            nbt = nbt,
        )

        entities.add(handle)
        return handle
    }

    fun tick(tracker: MenuInstance) {
        onTick.invoke(tracker)
        if (isDirty)
            spawn(tracker)
    }

    fun spawn(tracker: MenuInstance) {
        this.tracker = tracker
        onUpdate.invoke(tracker)
        val spawned = entities.map {
            tracker.spawn(it, NbtCompound().apply {
                put("Pos", it.pos.toNbt())
                it.nbt(this, tracker.ctx)
            })
        }
        isDirty = spawned.any { it == null }
    }

    fun despawn() {
        entities.forEach { tracker?.despawn(it) }
        onDespawn.invoke(Unit)
        this.tracker = null
    }
}
