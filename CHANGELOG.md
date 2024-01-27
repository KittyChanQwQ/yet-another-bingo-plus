### New Features:

- Added a `/bingo start` command to start games from the lobby

- Config files are now split into `config.json` (global config) and `game-options.json` (per-game options)

- Added "Reset Defaults" and "Re-roll Card" buttons to the lobby menu

- Added a `"preventLobbyChaos"` config option (default: false) that prevents players from making sounds and generally being a nuisance in the pre-game lobby

- Support for using [LuckPerms](https://minecraft.horrific.dev/bingo/mods/luckperms) and [Vanish](https://minecraft.horrific.dev/bingo/mods/vanish) to spectate games on multiplayer servers.

### Bugs Squashed:

- Various performance improvements to server-side map rendering & scoreboard updates

- Fixed bingo card not updating for other teams in the "Lockout" gamemode 
  
- Fixed a few potential crashes on client-side singleplayer games

- Fixed players not being teleported when the game starts if they change gamemode in the lobby

- Fixed rainy weather sometimes occurring in the pre-game lobby
