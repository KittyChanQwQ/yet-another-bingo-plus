### New Features:

- **Updated to support 1.21.5!**

- **Added a "Keep Playing" button!**

  Ever lost a game and wished you could complete the rest of the card anyway? Now you can!

- The "Game Over" screen now lists all items scored during the game!

- (1.20.6+) For inventory mode, items that are stored inside bundles, shulker boxes, or ender chests will remain on the card! 

- **Spectators now use an isolated chat** by default (meaning players can't see your messages!)

  You can change this in `config.json` with `"defaultToSpectatorChat": true`.

- (1.21.3+) Crafting recipe toasts are no longer shown when starting a game with "Unlock recipes" enabled

- Added `bingo_items`, `bingo_lines`, and `bingo_cards` scoreboards for tracking team scores via datapack

- (Client) Various HUD rendering optimizations

### Bugs Squashed:

- Fixed vanilla clients not being compatible with modded players hosting on LAN (due to the registered sound events)

- Fixed hidden items not being shown when the game is over

- Fixed card desync for spectators when players switch teams during a game

- Fixed scores not being shown to spectators when there's only one team

- Player statistics (and phantom/sleep timer) now properly reset after each (lobby mode) game

- Players can no longer whack each other during the countdown
