---
layout: doc
description: Use Voice Chat to automatically create team groups during Minecraft Bingo rounds.
---

# Voice Chat

When [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) is installed, the bingo mod will automatically create voice groups for each team at the start of the game.

::: warning Note:
Make sure to follow Simple Voice Chat's [Server Setup instructions](https://modrepo.de/minecraft/voicechat/wiki/server_setup) if you're using this on a server!
:::

- If there is only one player on each team, the mod will use a single "combined" group instead of creating separate team chats.
- Any spectating players (that are not on a team, or that join mid-round) will be placed in a separate "spectator" group.
- When the game ends, all players will be moved to the "combined" group.

This behavior can be changed in `yet-another-minecraft-bingo/config.json`...

```json
"integrations": {
	"voice": {
        "enabled": true,
        
        // Uses a single combined group when there is only one player per team
        "useCombinedGroupForSingleplayerTeams": true,
        
        // Uses a single combined group, regardless of the player/team count
        "useCombinedGroupAlways": false,
        
        // Group settings used for the per-team groups
        "teamGroups": {
            "name": "Bingo: %s Team", // "%s" will be replaced with the team name - e.g. "Bingo: Green Team"
            "type": "OPEN", // can be OPEN, NORMAL, or ISOLATED
            "password": null,
            "hidden": false
        },
        
        // Group settings used for the spectator group
        "spectatorGroup": {
            "name": "Bingo: Spectators",
            "type": "NORMAL",
            "password": null,
            "hidden": false
        },

        // Group settings used for the combined group
        "combinedGroup": {
            "name": "Bingo",
            "type": "OPEN",
            "password": null,
            "hidden": false
        }
    }
}
```

To tell the mod **not** to create a certain type of group, you can also set `"teamGroups": null`, `"spectatorGroup": null`, or `"combinedGroup": null`.

See the [additional config options](../config-extra#integrations) for more info.
