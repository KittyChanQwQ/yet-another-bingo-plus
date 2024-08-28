package me.jfenn.bingo.common.map

import me.jfenn.bingo.api.IPacketBuf
import me.jfenn.bingo.common.event.packet.PacketConverter
import me.jfenn.bingo.common.team.BingoPlayerProfile
import me.jfenn.bingo.common.team.BingoTeamKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import java.util.*

class CardUpdatePacket(
    val view: CardView,
) {

    object V1 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier.of("bingo", "update_card")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readString()
            val tilesLen = buf.readInt()
            val tiles = MutableList(tilesLen) { CardTile.V1.fromPacketBuf(buf) }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = Text.literal(teamName),
                    tiles = tiles,
                    players = emptyList(),
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeString(view.teamName.string)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                CardTile.V1.toPacketBuf(tile, dest)
            }
        }

    }

    object V2 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier.of("bingo", "update_card_v2")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readText()
            val tilesLen = buf.readInt()
            val tiles = MutableList(tilesLen) { CardTile.V2.fromPacketBuf(buf) }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = teamName,
                    tiles = tiles,
                    players = emptyList(),
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeText(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                CardTile.V2.toPacketBuf(tile, dest)
            }
        }

    }

    object V3 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier.of("bingo", "update_card_v3")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readText()
            val tiles = MutableList(buf.readInt()) { CardTile.V2.fromPacketBuf(buf) }
            val players = MutableList(buf.readInt()) {
                BingoPlayerProfile(
                    uuid = UUID.fromString(buf.readString()),
                    name = buf.readString(),
                )
            }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = teamName,
                    tiles = tiles,
                    players = players,
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeText(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                CardTile.V2.toPacketBuf(tile, dest)
            }

            dest.writeInt(view.players.size)
            for (player in view.players) {
                dest.writeString(player.uuid.toString())
                dest.writeString(player.name)
            }
        }
    }

    object V4 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier.of("bingo", "update_card_v4")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readText()
            val tiles = MutableList(buf.readInt()) { CardTile.V3.fromPacketBuf(buf) }
            val players = MutableList(buf.readInt()) {
                BingoPlayerProfile(
                    uuid = UUID.fromString(buf.readString()),
                    name = buf.readString(),
                )
            }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = teamName,
                    tiles = tiles,
                    players = players,
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeText(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                CardTile.V3.toPacketBuf(tile, dest)
            }

            dest.writeInt(view.players.size)
            for (player in view.players) {
                dest.writeString(player.uuid.toString())
                dest.writeString(player.name)
            }
        }
    }

    object V5 : PacketConverter<CardUpdatePacket> {
        override val id: Identifier = Identifier.of("bingo", "update_card_v5")!!

        override fun fromPacketBuf(buf: IPacketBuf): CardUpdatePacket {
            val teamKey = if (buf.readBoolean()) BingoTeamKey(buf.readString()) else null
            val teamColor = if (buf.readBoolean()) Formatting.valueOf(buf.readString()) else null
            val teamName = buf.readText()
            val tiles = MutableList(buf.readInt()) { CardTile.V4.fromPacketBuf(buf) }
            val players = MutableList(buf.readInt()) {
                BingoPlayerProfile(
                    uuid = UUID.fromString(buf.readString()),
                    name = buf.readString(),
                )
            }
            return CardUpdatePacket(
                CardView(
                    teamKey = teamKey,
                    teamColor = teamColor,
                    teamName = teamName,
                    tiles = tiles,
                    players = players,
                )
            )
        }

        override fun toPacketBuf(source: CardUpdatePacket, dest: IPacketBuf) {
            val view = source.view

            if (view.teamKey != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamKey.id)
            } else {
                dest.writeBoolean(false)
            }

            if (view.teamColor != null) {
                dest.writeBoolean(true)
                dest.writeString(view.teamColor.name)
            } else {
                dest.writeBoolean(false)
            }

            dest.writeText(view.teamName)

            // read-only copy of tiles to avoid concurrent modification
            val tiles = view.tiles.toList()
            dest.writeInt(tiles.size)
            for (tile in tiles) {
                CardTile.V4.toPacketBuf(tile, dest)
            }

            dest.writeInt(view.players.size)
            for (player in view.players) {
                dest.writeString(player.uuid.toString())
                dest.writeString(player.name)
            }
        }
    }

}