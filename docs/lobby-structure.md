# Lobby Structure

The lobby structure used as the game's spawn area can also be customized by adding an
[NBT structure file](https://minecraft.wiki/wiki/Structure_Block)
to the config.

These files can be created with a structure block, which will place a `.nbt` file in your world save, under
`.minecraft/saves/(WorldName)/generated/`. This file can then be copied to the BINGO
config directory, under `./config/bingo/lobby/lobby.nbt`.

```txt{4-5}
🗀 config/
 └ 🗀 bingo/
   ├ 🗀 images/
   ├ 🗀 lobby/
   │ └ lobby.nbt
   ├ config.json
   └ players.json
🗀 mods/
🗀 world/
server.properties
```

> If there are multiple files in this directory, one will be chosen at random each time
> the server starts up.

## Team Pickers

The "team picker" entities can be created anywhere in the structure, by placing a colored carpet on top of a lodestone
block.

This only works for the set teams included in the mod; i.e.
- Blue Team (`minecraft:blue_carpet`)
- Red Team (`minecraft:red_carpet`)
- Green Team (`minecraft:green_carpet`)
- Yellow Team (`minecraft:yellow_carpet`)
- Pink Team (`minecraft:pink_carpet`)
- Aqua Team (`minecraft:cyan_carpet`)
- Orange Team (`minecraft:orange_carpet`)
- Gray Team (`minecraft:light_gray_carpet`)

## Spawnpoint

The world spawn and menu placement are always at fixed locations, so these need to be kept in mind when
building the lobby structure.

The structure will be centered on `(0,0)`. The game will search for the highest solid block at `(0,0)`, which will be
used as the world spawn.

From the world spawn, the game menu is placed along `x=-5` at the same height. It has a height of 4 blocks and a width of 14.
