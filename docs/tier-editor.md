---
layout: doc
aside: false
description: Use this page to generate your own item tier lists to use with the Yet Another Minecraft BINGO mod. These files can be downloaded and placed in your server's config directory.
---

<script setup>
import Config from './components/Config.vue'
</script>

# Tier List Editor

Use this page to generate your own item tier lists to use with the mod. These files can be downloaded and placed in your server's config directory - located in `./config/yet-another-minecraft-bingo/overworld.tierlist.json`.

Any files ending with `.tierlist.json` in `./config/yet-another-minecraft-bingo/` will be shown as a tier list option in the game.

```txt{4-5}
🗀 config/
 └ 🗀 yet-another-minecraft-bingo/
   ├ 🗀 images/
   ├ overworld.tierlist.json
   └ nether.tierlist.json
🗀 mods/
🗀 world/
server.properties
```

The items below can be dragged between each column to sort them into tiers. Dragging an item back to "all items" will remove it from the list.

<Config />

