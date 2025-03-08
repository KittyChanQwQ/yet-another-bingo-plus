package me.jfenn.bingo.impl

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.modules.SerializersModule
import me.jfenn.bingo.client.platform.text.IText
import me.jfenn.bingo.platform.IJsonSerializers
import me.jfenn.bingo.platform.item.IItemStack
import me.jfenn.bingo.platform.item.IItemStackFactory
import net.minecraft.item.ItemStack
import net.minecraft.util.dynamic.Codecs
import org.slf4j.Logger
import kotlin.reflect.KClass

class JsonSerializers(
    private val itemStackFactory: IItemStackFactory,
    private val log: Logger,
) : IJsonSerializers {

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    inner class CodecSerializer<T: Any>(
        private val codec: Codec<T>,
        kClass: KClass<T>,
    ) : KSerializer<T> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("CodecSerializer(${kClass.qualifiedName})", SerialKind.CONTEXTUAL)

        override fun deserialize(decoder: Decoder): T {
            require(decoder is JsonDecoder)
            val jsonElement = decoder.decodeJsonElement()
            val jsonString = decoder.json.encodeToString(jsonElement)
            val gsonElement = JsonParser.parseString(jsonString)
            return codec.parse(JsonOps.INSTANCE, gsonElement).getOrThrow(false, log::error)
        }

        override fun serialize(encoder: Encoder, value: T) {
            require(encoder is JsonEncoder)
            val gsonElement = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow(false, log::error)
            val jsonString = gson.toJson(gsonElement)
            val jsonElement = encoder.json.decodeFromString<JsonElement>(jsonString)
            encoder.encodeJsonElement(jsonElement)
        }
    }

    private val module = SerializersModule {
        contextual(IText::class, CodecSerializer(
            codec = Codecs.TEXT.xmap(
                { TextImpl(it.copy()) },
                { it.value }
            ),
            kClass = IText::class,
        ))

        contextual(IItemStack::class, CodecSerializer(
            codec = ItemStack.CODEC.xmap(
                { itemStackFactory.forStack(it) },
                { it.stack }
            ),
            kClass = IItemStack::class,
        ))
    }

    override val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
        serializersModule = module
    }
}