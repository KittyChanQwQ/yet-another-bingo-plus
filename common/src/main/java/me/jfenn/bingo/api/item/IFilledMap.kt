package me.jfenn.bingo.api.item

interface IFilledMap : IItemStack {
    var mapId: Int?
    var mapColor: Int?
}