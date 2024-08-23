---
description: Find out how to configure Yet Another Minecraft BINGO using the options in its JSON config files.
---

# Config Files

The BINGO mod creates several config files the first time you start it up:

```txt{3-6}
🗀 config/
 └ 🗀 yet-another-minecraft-bingo/
   ├ config.json
   ├ game-options.json
   ├ game-options-default.json
   ├ player-settings.json
   └ ... .tierlist.json
🗀 mods/
🗀 world/
server.properties
```

## `config.json`

A few config options _can_ be changed in `config.json` to adjust some specific behavior, such as specific scoring settings or shutdown logic.
The specific options are described [on the "Additional Options" page](./config-extra).

## `game-options.json`

Most of these options can be changed in-game, using the menu in the pre-game lobby. For the most part, this file does not need to be manually edited.

This file is overwritten with the selected options whenever a game is started.

## `game-options-default.json`

This file supplies the default options for the game menu - used whenever the "Reset Defaults" button is pressed. If you want to change the default game options, this file can be edited manually - or copied from `game-options.json`.

## `player-settings.json`

`player-settings.json` contains player-specific settings, such as whether to display info in a bossbar or scoreboard. These options can be changed by each player in the pre-game lobby.

## `*.tierlist.json`

The tier list files are used to generate the BINGO card from a set of [tier lists](./tier-editor). These are randomized for each game according to the configured [item distribution](./game-options#item-distribution).

Multiple tier lists can be used at once! Any file ending with `.tierlist.json` will be shown as an option in the game.

