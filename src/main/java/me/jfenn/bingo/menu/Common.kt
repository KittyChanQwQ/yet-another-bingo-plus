package me.jfenn.bingo.menu

import me.jfenn.bingo.event.InteractionEntityEvents
import me.jfenn.bingo.utils.EventListener
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

const val MENU_HITBOX_HEIGHT = 0.25f
const val MENU_HITBOX_WIDTH = 1f

const val MENU_ITEM_OFFSET = 0.5
const val MENU_ITEM_HEIGHT = 0.3
interface Property<T> {
    val subscribe: EventListener<T>
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
}

class MutableProperty<T>(
    private var value: T
) : Property<T> {
    override val subscribe = EventListener<T>()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        subscribe.invoke(value)
    }

    companion object {
        fun <T> ref(prop: KMutableProperty0<T>): MutableProperty<T> {
            val property = MutableProperty(prop.get())
            property.subscribe { prop.set(it) }
            return property
        }
    }
}

class DerivedProperty<T>(
    private var getter: () -> T
) : Property<T> {
    override val subscribe = EventListener<T>()

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return getter()
    }
}

data class MenuEntityHandle<T: Entity>(
    val id: UUID = UUID.randomUUID(),
    val type: EntityType<T>,
    val nbt: NbtCompound.() -> Unit = {},
    val onUpdate: EventListener<T> = EventListener(),
)

fun component(setup: MenuComponent.() -> Unit): MenuComponent {
    val component = MenuComponent()
    component.setup()
    return component
}

open class MenuComponent {

    private var tracker: MenuTracker? = null
    val entities: MutableList<MenuEntityHandle<*>> = mutableListOf()

    val onUpdate = EventListener<MenuTracker>()
    val onDespawn = EventListener<Unit>()

    fun <P: Property<*>> property(property: P) = property.apply {
        subscribe {
            tracker?.let { spawn(it) }
        }
    }

    fun <T> computedProperty(getter: () -> T): Property<T> {
        // this assumes that anything used in the getter is already observed
        // so this does not need to add any listeners
        return DerivedProperty(getter)
    }

    fun <T: Entity> registerEntity(type: EntityType<T>, nbt: NbtCompound.() -> Unit = {}): MenuEntityHandle<T> {
        val handle = MenuEntityHandle(
            id = UUID.randomUUID(),
            type = type,
            nbt = nbt,
        )

        entities.add(handle)
        return handle
    }

    open fun spawn(tracker: MenuTracker) {
        this.tracker = tracker
        onUpdate.invoke(tracker)
        entities.forEach { tracker.spawn(it, NbtCompound().apply(it.nbt)) }
    }

    open fun despawn() {
        entities.forEach { tracker?.despawn(it) }
        onDespawn.invoke(Unit)
        this.tracker = null
    }
}

data class MenuTracker(
    val world: ServerWorld,
) {

    companion object {
        val log = LoggerFactory.getLogger(this::class.java)!!
    }

    fun <T: Entity> spawn(info: MenuEntityHandle<T>, nbt: NbtCompound = NbtCompound()): T? {
        var entity: T? = world.getEntity(info.id) as? T

        if (entity == null) {
            entity = info.type.create(world)!!
            entity.uuid = info.id
            world.spawnEntity(entity)
        }

        if (entity.type != info.type) {
            log.error("Entity type ${info.type} does not match the entity being updated")
            return null
        }

        entity.readNbt(nbt)
        info.onUpdate(entity)
        return entity
    }

    fun despawn(info: MenuEntityHandle<*>) {
        val entity = world.getEntity(info.id)
        if (entity != null && entity.type == info.type) {
            InteractionEntityEvents.INTERACT_LISTENERS.remove(info.id)
            entity.kill()
        } else {
            log.error("Entity type ${info.type} does not match the entity being despawned")
        }
    }

}
