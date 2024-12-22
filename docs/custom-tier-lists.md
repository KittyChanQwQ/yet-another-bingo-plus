# Datapack Tier Lists

A tier list can be bundled with a datapack for easier distribution. This is useful if you want to combine custom advancements, scoreboards, or other functionality to share it as a single datapack zip.

You can copy a tier list directly from the [tier list editor](./tier-editor) into `data / <namespace> / yet-another-minecraft-bingo / tierlists / <name>.tierlist.json`, or create the file manually:

```json
{
  "S": [],
  "A": [],
  "B": [],
  "C": [],
  "D": [],
  "groups": []
}
```

Each objective in the list can refer to an **item**, an **advancement**, or a [custom objective](./custom-objectives) ID.

For example, `"minecraft:stone"` will resolve to the stone item, whereas `"minecraft:story/mine_stone"` is an advancement.

Additionally, [item tags](https://minecraft.wiki/w/Tag#Item_tags) can be used to substitute multiple items into the same list; e.g. `"#minecraft:leaves"`.

## Groups

Tier list "groups" are intended as a way to prevent similar or conflicting objectives from appearing on the same card.

This also weights the list as a whole rather than the individual objectives when generating a card, so that they don't appear any more frequently than other individual items.

Given the amount of variations of copper, cut copper, oxidized copper slabs, waxed lightly weathered cut copper stairs... if all of those were included in a list _without_ placing them in a group, you would be guaranteed to get an entire card of copper variants on most rolls.

```json
  "groups": [
    [
      "minecraft:copper_block",
      "minecraft:cut_copper",
      "minecraft:exposed_cut_copper",
      ...
    ]
  ]
```

:::info

Objectives in the same group can be placed on different tiers. It will also pick up items that are nested in [custom objectives](./custom-objectives) - e.g. if a `"one_of"` objective includes a `"minecraft:copper_block"`.

While groups behave similarly to the `"conflictsWith"` property on custom objectives, note that `"conflictsWith"` does _not_ affect the weighting of the list.

:::
