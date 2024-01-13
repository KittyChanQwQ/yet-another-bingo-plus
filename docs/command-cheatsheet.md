# Command Cheatsheet

## Player commands

| Command | Description |
|---------|-------------|
| `/coords` | Prints the player's current coordinates in the team chat. |
| `/coords list` | Displays a list of the team's previous "/coords" messages. |
| `/all` or `/g` | Sends a message outside of the team chat. |
| `/bingo set <option> <true/false>` | Changes a player setting (e.g. `scoreMessages` or `scoreboardAutoHide`). These options are also shown in the lobby. |

## Operator-only commands

| Command | Description |
|---------|-------------|
| `/bingo shuffleteams #` | Randomly shuffles all players onto the given number of teams. |
| `/bingo elytra <true/false>` | Changes whether players have elytra/rockets during the game. |
| `/bingo timelimit #` | Sets the time limit of the game, in minutes (or "off" for no timer) |
| `/bingo wincon <option>` | Changes the number of Bingo lines needed for a team to win the game. |
| `/bingo itemdist # # # # #` | Changes the [item distribution](./item-distribution) used to generate the card. |
| `/bingo reroll` | Re-rolls the card for completely new random items. |
| `/bingo end` | Immediately ends the current game and displays the winner. |
| `/bingo seed` | Shows the random seed used to generate the current Bingo Card. |
| `/bingo seed #` | Sets the Bingo Card seed to a specific value. |
| `/join <team> <player>` | Assigns a player to a different team - will respawn them if in-game. |
