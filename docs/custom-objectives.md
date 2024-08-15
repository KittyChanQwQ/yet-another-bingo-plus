# Custom Objectives

By default, any advancement or item in the game can be used directly in a tier list.

However, you might want a particular item or objective to have different behavior when it appears on a card. This
includes requiring specific NBT data, a certain quantity of an item, multiple items at once, and so on.

This can be achieved by placing custom `.json` objectives in a
`data / <namespace> / yet-another-minecraft-bingo / objectives / <goal>.json` file within a datapack.

To see an example, the built in ["challenge"](https://gitlab.com/horrific-tweaks/bingo/-/tree/main/common/src/main/resources/data/yet-another-minecraft-bingo/yet-another-minecraft-bingo/objectives/challenge)
objectives can be used as a reference - which are included in [challenge.tierlist.json](https://gitlab.com/horrific-tweaks/bingo/-/blob/main/common/src/main/resources/yet-another-minecraft-bingo/challenge.tierlist.json).

### Number Source

Some of these JSON properties describe a "Number Source", which can be one of three values:
- Constant: `"count": 8` - providing a direct number to use for the value
- Options: `"count": [8, 16, 32]` - providing an array of options from which a number will be randomly chosen
- Randomized: `"count": { ... }` - providing a min/max from which a random number will be generated
  - `"type": "random"`
  - `"min": 8`: The lower boundary for the item count (inclusive)
  - `"max": 32`: The upper boundary for the item count (inclusive)

## Objective JSON

These are the base properties that exist on every objective:

- `"type": "..."`: The type of objective: `item`, `one_of`, `all_of`, `inverse`, or `opponent`.
- `"display": {}`: Alters what name or item the objective shows when displayed on the card
  - `"name": {"text": "Kelp!"}`: Uses [raw JSON text format](https://minecraft.wiki/w/Raw_JSON_text_format) to specify the objective name
  - `"item": "minecraft:kelp"`: Changes the visual item for the objective
  - `"itemNbt": "{}"`: Sets pre-1.20.6 NBT data on the item
  - `"itemComponents": {}`: Sets post-1.20.6 JSON-encoded [data components](https://minecraft.wiki/w/Data_component_format#List_of_components) on the item
    - `"minecraft:enchantments": {"levels":{"mending":1}}`
  - `"decoration": "NONE"`: Applies a decoration to the objective's tile on the card. One of: `ADVANCEMENT`, `MULTI_ITEM`, `FORBIDDEN`, `NONE`.
- `"conflictsWith: ["minecraft:dried_kelp"]`: A list of conflicting objectives that should never appear on the same card as this objective.
 
## Items

- `"type": "item"`
- `"item": "minecraft:kelp"`: The item that the player needs to pick up for this objective
- [`"count": <number-source>`](#number-source): The amount of this item that the player needs to obtain
- `"nbt": "{}"`: Sets pre-1.20.6 NBT data on the item
- `"components": {}`: Sets post-1.20.6 JSON-encoded [data components](https://minecraft.wiki/w/Data_component_format#List_of_components) on the item
  - `"minecraft:enchantments": {"levels":{"mending":1}}`

## Advancements

- `"type": "advancement"`
- `"advancement": "minecraft:story/mine_stone"`: The [advancement identifier](https://minecraft.wiki/w/Advancement#List_of_advancements) that should capture the objective when obtained.

## Statistics

- `"type": "stats"`
- `"statType": "minecraft:custom`: Specifies the [statistic type](https://minecraft.wiki/w/Statistics).
- `"statName": "minecraft:open_barrel"`: The [statistic name](https://minecraft.wiki/w/Statistics). Optional - if omitted, uses the sum of all statistics under the type.
- [`"min": <number-source>`](#number-source): The minimum statistic value required to earn this objective (default: 1)
- [`"max": <number-source>`](#number-source): The maximum statistic value required to keep this objective (default: unset)
- `"relative": true`: Measures the increase in this statistic since the start of the game, rather than its actual value. (default: true)
  - _If false, you should ensure that this cannot be scored in the lobby or before the game has started._

## Scoreboard Objectives

- `"type": "scoreboard"`
- `"scoreboardName": "your_datapack_objective"`: The name of the [scoreboard objective](https://minecraft.wiki/w/Scoreboard) that should be tracked.
- [`"min": <number-source>`](#number-source): The minimum scoreboard value required to earn this objective (default: 1)
- [`"max": <number-source>`](#number-source): The maximum scoreboard value required to keep this objective (default: unset)
- `"relative": false`: Measures the increase in this scoreboard since the start of the game, rather than its actual value. (default: false)
  - If your scoreboard represents a statistic/counter, this can avoid needing to reset its value when the game starts.
    Otherwise, you should use the bingo [function tags](./functions) to reset all scoreboard values when `on_playing` is run.

## One Of ...

- `"type": "one_of"`
- `"objectives": ["minecraft:kelp", "minecraft:dried_kelp"]`: A list of objective IDs. The player may obtain any of these to complete this objective.

## All Of ...

- `"type": "all_of"`
- `"objectives": ["minecraft:kelp", "minecraft:dried_kelp"]`: A list of objective IDs. The player must obtain all of these to complete this objective.

## Inverse / Never Obtain ...

- `"type": "inverse"`
- `"objective": "minecraft:wheat_seeds"`: The objective that should be inverted. For example, this goal will be achieved by every team until they capture wheat seeds.

## Opponent Obtains ...

- `"type": "opponent"`
- `"objective": "minecraft:wheat_seeds"`: The objective that should be tracked. For example, when a team obtains wheat seeds, every other team will capture this objective.
