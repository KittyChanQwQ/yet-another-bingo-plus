# Config Files

The BINGO mod creates several config files the first time you start it up:

```txt{3-5}
🗀 config/
 └ 🗀 bingo/
   ├ ... .tierlist.json
   ├ config.json
   └ players.json
🗀 mods/
🗀 world/
server.properties
```

## `config.json`

Most config properties can be changed in-game, using the menu in the pre-game lobby. For the most part, this file does not need to be manually edited.

A few [additional config options](./config-extra) _can_ be changed in `config.json` to adjust some specific behavior, such as spawn distance or shutdown logic.

## `players.json`

`players.json` contains player-specific settings, such as whether to display info in a bossbar or scoreboard. These options can be changed by each player in the pre-game lobby.

## `*.tierlist.json`

The tier list files are used to generate the BINGO card from a set of [tier lists](./tier-editor). These are randomized for each game according to the configured [item distribution](./item-distribution).

Multiple tier lists can be used at once! Any file ending with `.tierlist.json` will be shown as an option in the game.

