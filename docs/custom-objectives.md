# Custom Objectives

By default, any advancement or item in the game can be used directly in a tier list.

However, you might want a particular item or objective to have different behavior when it appears on a card. This
includes requiring specific NBT data, a certain quantity of an item, multiple items at once, and so on.

This can be achieved by placing custom `.json` objectives in a
`data / <namespace> / yet-another-minecraft-bingo / objectives / <goal>.json` file within a datapack.

To see an example, the built in ["challenge"](https://gitlab.com/horrific-tweaks/bingo/-/tree/main/common/src/main/resources/data/yet-another-minecraft-bingo/yet-another-minecraft-bingo/objectives/challenge)
objectives can be used as a reference - which are included in [challenge.tierlist.json](https://gitlab.com/horrific-tweaks/bingo/-/blob/main/common/src/main/resources/yet-another-minecraft-bingo/challenge.tierlist.json).

## Objective JSON

These are the base properties that exist on every objective:

- `"type": "..."`: The type of objective: `item`, `one_of`, `all_of`, `inverse`, or `opponent`.
- `"display": {}`: Alters what name or item the objective shows when displayed on the card
  - `"name": {"text": "Kelp!"}`: Uses [raw JSON text format](https://minecraft.wiki/w/Raw_JSON_text_format) to specify the objective name
  - `"item": "minecraft:kelp"`: Changes the visual item for the objective
  - `"itemNbt": "{}"`: Sets pre-1.20.6 NBT data on the item
  - `"itemComponents": {}`: Sets post-1.20.6 JSON-encoded [data components](https://minecraft.wiki/w/Data_component_format#List_of_components) on the item
    - `"minecraft:enchantments": {"levels":{"mending":1}}`
- `"conflictsWith: ["minecraft:dried_kelp"]`: A list of conflicting objectives that should never appear on the same card as this objective.

### `"type": "item"`

- `"item": "minecraft:kelp"`: The item that the player needs to pick up for this objective
- `"count": {}`: Provides the amount of this item that the player needs to obtain
  - Constant: `"count": 64` 
  - Randomized:
    - `"type": "random"`
    - `"min": 8`: The lower boundary for the item count (inclusive)
    - `"max": 32`: The upper boundary for the item count (inclusive)
- `"nbt": "{}"`: Sets pre-1.20.6 NBT data on the item
- `"components": {}`: Sets post-1.20.6 JSON-encoded [data components](https://minecraft.wiki/w/Data_component_format#List_of_components) on the item
  - `"minecraft:enchantments": {"levels":{"mending":1}}`

### `"type": "one_of"`

- `"objectives": ["minecraft:kelp", "minecraft:dried_kelp"]`: A list of objective IDs. The player may obtain any of these to complete this objective.

### `"type": "all_of"`

- `"objectives": ["minecraft:kelp", "minecraft:dried_kelp"]`: A list of objective IDs. The player must obtain all of these to complete this objective.

### `"type": "inverse"`

- `"objective": "minecraft:wheat_seeds"`: The objective that should be inverted. For example, this goal will be achieved by every team until they capture wheat seeds.

### `"type": "opponent"`

- `"objective": "minecraft:wheat_seeds"`: The objective that should be tracked. For example, when a team obtains wheat seeds, every other team will capture this objective.
