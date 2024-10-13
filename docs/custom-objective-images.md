# Custom Objective Images

To use a custom icon on your bingo objective, you'll need to save it as a 16x16 PNG image, under
`data / <namespace> / yet-another-minecraft-bingo / images / <path>.png`.

While larger images *can* be displayed in the mod, the vanilla map is limited to 16x16px. If you use a larger image, it may appear pixelated for players that do not have the client mod/overlay.

## Example

`data / mynamespace / yet-another-minecraft-bingo / images / funny_cat.png`

::: code-group

```json [objectives / kitten_booped.json]
{
  "type": "stats",
  "display": {
    "name": "Boop the kitty",
    "image": "mynamespace:funny_cat"
  },
  "statType": "minecraft:custom",
  "statName": "minecraft:kitten_booped"
}
```

:::

To see more examples, the built in ["challenge"](https://gitlab.com/horrific-tweaks/bingo/-/tree/main/common/src/main/resources/data/yet-another-minecraft-bingo/yet-another-minecraft-bingo/objectives/challenge)
objectives can be used as a reference - such as [`stat_killed_by_warden`](https://gitlab.com/horrific-tweaks/bingo/-/blob/main/common/src/main/resources/data/yet-another-minecraft-bingo/yet-another-minecraft-bingo/objectives/challenge/stat_killed_by_warden.json).
