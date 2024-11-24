### New Features:

- **The mod no longer needs to recreate the entire world save** to start a game with new terrain.

  This means that if you're using Essential, players can stay in the same lobby for multiple games! Additionally, dedicated servers no longer need to restart when the game is over.

  ** This _may_ break compatibility with a few mods. If you encounter such issues, please let me know! I've added a `nextRoundCompatibilityMode` config to turn it off, if needed. 

- **Expanded the built-in lobby world!** It's bigger.

  (thanks BlueberryBrown for help building secrets!)

- **Replaced the tier list settings with "filters"** - these work mostly the same, but enable more customization with the following commands:

  * `/bingo filter preset everything` - Replaces all filters with the "everything" preset (equivalent to `exclude unobtainable` & `exclude tedious`)
  * `/bingo filter only in_overworld` - Selects only items that are obtainable in the overworld
  * `/bingo filter exclude tedious` - Excludes any items that are in the "tedious" list
  * `/bingo filter count "type=advancement" 5` - Ensures that exactly 5 advancements are on the card (if possible).

- Added "ready" indicators to the player tab list

- Improved some unintuitive edge cases with the "Win Behavior" settings

### Bugs Squashed:

- Fixed the "Reset Defaults" button just not working whatsoever

- Fixed spawn kit items sometimes not stacking with crafted items ([#114](https://gitlab.com/horrific-tweaks/bingo/-/issues/114))

- Fixed custom objective icons not showing up on score notifications

### Translations

- New Chinese Simplified translations by Fishlanding!
- New Polish translations by Toniemax!
- New French translations by Yosh64!
- New Portuguese translations by Lucasofchirst!
