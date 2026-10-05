package me.jfenn.bingo.impl.item

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.mojang.serialization.JsonOps
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackComponentOps
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("bingo")

class ItemStackComponentOpsImpl(
    server: MinecraftServer,
) : IItemStackComponentOps {
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    private val jsonOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)

    override fun setComponentsString(stack: IItemStack, components: Map<String, String?>): Boolean {
        require(stack is ItemStackImpl)
        var isSuccessful = true

        for ((key, value) in components) {
            val id = Identifier.tryParse(key)
            val type = id?.let { BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(it) }

            if (id == null || type == null) {
                logger.error("[ItemStackFactory] Error parsing item component $key for ${stack.item.descriptionId}: not a valid identifier")
                isSuccessful = false
                continue
            }

            if (value == null) {
                stack.stack.remove(type)
                continue
            }

            val jsonElement = gson.fromJson(value, JsonElement::class.java)
            if (jsonElement.isJsonNull) {
                stack.stack.remove(type)
                continue
            }

            val component = try {
                type.codecOrThrow()
                    .decode(jsonOps, jsonElement)
                    .getOrThrow()
                    .first
            } catch (e: Throwable) {
                logger.error("[ItemStackFactory] Error parsing item component $key for ${stack.item.descriptionId}", e)
                isSuccessful = false
                null
            }

            stack.stack.set(@Suppress("UNCHECKED_CAST") (type as DataComponentType<Any>), component)
        }

        return isSuccessful
    }

    override fun getComponentsString(stack: IItemStack): Map<String, String?>? {
        require(stack is ItemStackImpl)
        val components = mutableMapOf<String, String?>()
        val patch = stack.stack.componentsPatch
        val componentRegistryStream = BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet()
            .map { (key, value) -> key.identifier() to @Suppress("UNCHECKED_CAST") (value as DataComponentType<Any>) }

        for ((id, componentType) in componentRegistryStream) {
            if (componentType.isTransient) continue
            val component = patch.get(stack.stack, componentType) ?: continue

            val jsonElement = try {
                TypedDataComponent(componentType, component)
                    .encodeValue(jsonOps)
                    .getOrThrow()
                    .let { gson.toJson(it) }
            } catch (e: Throwable) {
                logger.warn("Unable to encode component '${id}'", e)
                null
            }

            if (jsonElement != null) {
                components[id.toString()] = jsonElement
            }
        }

        return components.takeIf { it.isNotEmpty() }
    }

    private fun gsonOverlapsRecursive(predicate: JsonElement, actual: JsonElement): Boolean {
        if (predicate.isJsonNull && actual.isJsonNull) {
            return true
        }
        if (predicate.isJsonPrimitive && actual.isJsonPrimitive) {
            return predicate.asString == actual.asString
        }
        if (predicate.isJsonArray && actual.isJsonArray) {
            val predicateArr = predicate.asJsonArray
            val actualArr = actual.asJsonArray
            return predicateArr.all { predicateItem ->
                actualArr.any { actualItem -> gsonOverlapsRecursive(predicateItem, actualItem) }
            }
        }
        if (predicate.isJsonObject && actual.isJsonObject) {
            val predicateObj = predicate.asJsonObject
            val actualObj = actual.asJsonObject
            return predicateObj.keySet().all {
                gsonOverlapsRecursive(predicateObj.get(it) ?: JsonNull.INSTANCE, actualObj.get(it) ?: JsonNull.INSTANCE)
            }
        }
        return false
    }

    override fun isDataOverlapping(stack: IItemStack, components: Map<String, String?>?): Boolean {
        if (components == null) return true

        val itemComponents = getComponentsString(stack)
            ?.mapValues { (_, value) -> value?.let { gson.fromJson(it, JsonElement::class.java) } }
            ?: return components.isEmpty()

        val predicateComponents = try {
            components.mapValues { (_, value) ->
                value?.let { gson.fromJson(it, JsonElement::class.java) }
            }
        } catch (e: Throwable) {
            logger.error("[ItemStackFactory] Error parsing item components for ${stack.item.descriptionId} predicate", e)
            return false
        }

        return predicateComponents.all { (key, predicateComponent) ->
            val itemComponent = itemComponents[key]?.takeIf { !it.isJsonNull }
            when {
                predicateComponent == null -> itemComponent != null
                itemComponent == null -> false
                else -> gsonOverlapsRecursive(predicateComponent, itemComponent)
            }
        }
    }
}