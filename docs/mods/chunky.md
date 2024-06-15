---
layout: doc
description: Use Chunky with the Yet Another Minecraft BINGO mod to pre-generate your bingo worlds!
---

# Chunky

The [Chunky mod](https://modrinth.com/plugin/chunky) can pre-generate Minecraft worlds up to a specified distance. This can prevent lag due to chunk generation while moving around the world.

However, it does need time to run before starting a game, and can use a fair amount of storage space.

The default configuration will generate a distance of 8000 blocks in the overworld, and 1000 in the nether.

Pre-generation will begin as soon as the server starts up. Any pre-generation tasks will be cancelled as soon as you start the game, so it will not have any effect on gameplay.

::: warning Warning:
Make sure that you have enough storage space to do this! The default settings will need at least **12GB of disk space** to fully pre-generate the world.
:::

The pre-generation worlds/distance can be changed in the `yet-another-minecraft-bingo/config.json` config. It can also be turned off entirely, if you'd prefer to run the Chunky commands yourself.

```json
"integrations": {
	"chunky": true,
	"chunkyWorlds": {
        "minecraft:overworld": 8000,
        "minecraft:the_nether": 1000
    }
}
```

See the [additional config options](../config-extra#integrations) for more info.
