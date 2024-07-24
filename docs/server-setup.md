---
description: Configure Minecraft BINGO on a server in either SMP or Lobby mode.
---

# Server Setup

When the mod is installed on a dedicated server, it can be used in two "modes":

<table>
<thead>
<tr>
<th>SMP Mode (Default)</th>
<th>Lobby Mode</th>
</tr>
</thead>
<tbody>
<tr>
<td>

- Behaves like a survival server
- Does not teleport players, clear inventory, or change gamemode
- Has limited game options
- Does not erase any world data

</td>
<td>

- Erases the world on every launch
- Starts players in a pre-game lobby structure
- Teleports teams into the world with an empty inventory

</td>
</tr>
</tbody>
</table>

## SMP Mode

**SMP Mode** is the default because it involves minimal changes to the game and will not erase any player data.

This means that the mod can be installed on an SMP to run a game as a server event.

There is no "pre-game lobby" in this mode. Instead, you can configure and start the game using the `/bingo` commands.

This will not interfere with other players on the server, and is heavily limited in functionality to avoid any game-breaking behavior.

::: warning
**Please take regular backups if you care about your world!**

This mode has been tested to ensure that world files and player inventory are kept safe, but computers are quite complicated so I can't make any guarantees.

There is no way to recover your world data if it is deleted by this mod.
:::

## Lobby Mode

On the other hand, **Lobby Mode** is intended for servers that will *only* run BINGO games. This mode **resets the entire world after each game!**

In Lobby Mode, the mod will start players in the BINGO lobby and teleport them to random locations when the game is started.

To enable Lobby mode:
- Run the `/bingo lobby` command. This will warn you about the world deletion behavior.
- Acknowledge the warnings with `/bingo lobby confirm`.
- Wait for the server to restart.

