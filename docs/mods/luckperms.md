---
layout: doc
aside: false
---

# LuckPerms

This mod uses a set of permission checks for most of its behavior - which can be configured through a permissions mod such as [LuckPerms](https://modrinth.com/mod/luckperms) (or any other permissions mod using the [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api)).

::: info
A permissions mod is not required - the game will run fine without it using the default permission values. However, these may be useful if you want to grant players the ability to start games without giving them full OP access on your server.
:::

Check out the [LuckPerms Wiki](https://luckperms.net/wiki/Home) for how to set up and configure these permissions!

| Permission | Default | Description |
|------------|---------|-------------|
| `bingo.configure.game` | Operators | Interact with the game menu in the lobby, and configure the game through commands. |
| `bingo.configure.player` | Everyone | Interact with the player menu in the lobby, and use the `/bingo set` commands. |
| `bingo.command.join` | Everyone | Use the `/join` command to join a team in the lobby. |
| `bingo.command.join_player` | Operators | Use the `/join` command to change another player's team. |
| `bingo.command.coords` | Everyone | Use the `/coords` command to share coordinates with the team. |
| `bingo.command.spectator` | Everyone | Use the `/spectator` command (when spectating a game, if [vanished](./vanish)) to switch between spectator and adventure gamemodes. |
| `bingo.command.restart` | Operators | Use the `/restart` command to restart the server. |
| `bingo.command.bingo` | Operators | Use the `/bingo` command to change options or end the game. |
| `bingo.command.bingo_set` | Everyone | Use the `/bingo set` command to change player-specific bossbar/scoreboard settings. |
| `bingo.lobby.bypass_chaos_prevention` | Operators | Whether the player can bypass the "lobbyChaosPrevention" setting and make sounds in the lobby. |
| `bingo.spectator.use_doors` | Noone | Whether a spectator (if [vanished](./vanish) in adventure mode) can open doors in the world. |
| `bingo.spectator.view_inventory` | Everyone | Whether a spectator can look inside chests during a game. |
| `bingo.spectator.view_cards` | Everyone | Whether a spectator is given every team's cards during a game. |
