---
description: Minecraft BINGO is often a lot more demanding on the server than regular gameplay! Find out how you can configure the game to run at its best performance.
---

# Server Optimization

Minecraft BINGO is often a lot more demanding on the server than regular gameplay! The game sometimes doesn't run very well, especially if you're playing on a mid-range server with lots of players... and in a game where the objective is to explore as much as possible.

Ideally, you should look for a server to host on with at least 6GB RAM and adequate CPU power.

## JVM Flags

I strongly recommend using [Aikar's Flags](https://mcflags.emc.gs) on any installation of Minecraft. The [Paper Documentation](https://docs.papermc.io/paper/aikars-flags) has some information on setting these up, or you can copy the flags below:

```
-Xms10G -Xmx10G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true
```

> **Note that `-Xms10G -Xmx10G` will need to be changed** depending on your available memory.

> For example, if your host has 6GB of RAM, try setting these to `-Xms5G -Xmx5G` - you will need to provide a bit of overhead for other programs on the system. 

## Optimization Mods

This is a list of server-side performance mods I've been using with a fair amount of success:

- [Lithium](https://modrinth.com/mod/lithium) is a fairly standard optimization mod that doesn't change any game behavior.

- [FerriteCore](https://modrinth.com/mod/ferrite-core) adds some widely-compatible memory usage enhancements.

- [ServerCore](https://modrinth.com/mod/servercore) has a few improvements based on PaperMC, Purpur, and Airplane. A lot of these are turned off by default - I recommend only setting `fast_biome_lookups = true` on its default config to start with.

- [MC-249136 Fix](https://modrinth.com/mod/mc-249136-fix) is a mod I wrote to fix a specific lag spike with buried treasure. Buried treasure maps will still take just as long to load, but they won't freeze (and potentially crash) the entire server while they do so.

- [Krypton](https://modrinth.com/mod/krypton) makes a lot of changes to server networking that I've seen improvements from. If you're using a heavily modded server, you may want to leave this out, but it should work with most installs. See the [Krypton FAQ/Wiki](https://github.com/astei/krypton/wiki/FAQ) for more information.

- [C2ME](https://modrinth.com/mod/c2me-fabric) is in alpha, but drastically speeds up chunk loading, which makes elytra mode a lot more playable.

  > Note that while the server loads *more chunks* with this mod, I have not seen it significantly affect lag or tick speed. This likely won't change any lag/responsiveness issues on your server. Additionally, being in alpha, it does have some quirks that seem to (very occasionally) cause broken chunks or invisible entities.

  > As such, if you are not using elytra mode and do not need to stress the chunk loading, I would recommend leaving this out for better stability.

- [Chunky](https://modrinth.com/plugin/chunky) can be used to pre-generate the world files, which prevents lag to due to world generation. However, it does need time to run before starting a game, and can use a fair amount of storage space.

  See the [Chunky integration](./mods/chunky) page for more info!

## Auto-save

Occasionally, you might notice a lag spike accompanied by a chat message that the game is saving. This can happen if your server is on an HDD instead of an SSD, or has a slow disk write speed.

If your server has plenty of RAM (for vanilla, probably about `10G` or higher), then you can try turning off auto-save to avoid these lag spikes.

- Minecraft does not have an option to configure the auto-save interval by default.
- Your hosting provider might have an option for this (or an "auto save interval" config) that you can turn off or set to "0".
- Otherwise, you can turn this off by running `/save-off` before starting a game.

This means that the server **will not save the game** while you are playing. However, this should not affect most bingo games, as you aren't keeping the world afterwards anyway!

If you start to see **constant lag** at some point during a game, then your server is likely running out of memory due to not saving its chunks! You'll want to turn auto-save back on if that is the case. 

### Shutdown Behavior

If you've turned off auto-save, the server will normally try to save all chunks at once while shutting down. If this takes an excessive amount of time, you can configure [`"unsafeSkipWorldClose": true`](./config-extra#unsafeskipworldclose) in `/config/bingo/config.json` to skip this behavior.

## Server Profiling

If you've done all this and you *still* have lag issues, you might just need a more powerful machine - but it's worth checking a few things first:

### What is the lag?

These are a few types of "lag issues" that I've encountered in the game - it's good to categorize these when looking for a solution:

- **Client-side lag:** things that only happen for one player, or don't appear to have any effect on the server. For example, players might complain about rain causing lag; this is common on low-end graphics cards, and is not an issue with the server.

- **Desync issues:** inconsistent game behavior that can be observed under a lot of lag or with buggy networking (which might be a connection issue or incompatible mods). This includes rubber-banding, blocks that won't let you break them, and inventory items disappearing, which are usually fixed after logging off and back on.

- **Lag spikes:** an occasional pause where the server cannot complete a task in the expected time, and needs to hold everything up until it's done. This is usually caused by a specific action or event (such as a player entering the Nether), and the server can recover from it perfectly fine. These can become less noticeable with a more powerful server, but there is usually a root cause to these that can be fixed.

- **Constant lag:** *everything you do* is delayed, and the server cannot complete its normal behavior in a reasonable time. This might indicate some kind of configuration issue, or that your machine just isn't powerful enough to run the game. You might want to look into reducing the server `view-distance` and `simulation-distance`, or getting a more powerful machine.

### Spark

I recommend installing [spark](https://modrinth.com/mod/spark) to capture what your server is doing when running the game. You can use `/spark profiler open` to get a bunch of information about what's taking up CPU time.

Note that a mod with a higher "Server thread" percentage does not necessarily mean that it is causing more lag. For example, Lithium usually gets a high percentage because it replaces a lot of server logic that would otherwise take a *longer time* to run.

Here are some of the numbers I'd recommend looking at:
- Compare the "TPS" and "CPU (process)" graphs. Does the TPS drop at the same time as high CPU load? If the CPU is close to maxed out, that indicates that your system/host might not be powerful enough.

- Look at "Memory (process)" - unfortunately, I don't think this is recorded over time, so it only shows the most recent value. If this reaches 100%, you either need more RAM or something is allocating too many resources.

- You might also want to look at the "Chunks" and "Entities" graphs. The [PTA Gamers](https://ptagamers.com) server stays around 20,000 chunks and 2,000 entities in an 8 player game (this may vary with server `view-distance`). These graphs should not show sharp increases/decreases unless players leave/join the game partway through.

- Check the four areas labelled as "GC". These measure whenever the JVM pauses to run garbage collection and clean up its memory allocations.

  If these average more than 100ms, they may be causing noticeable lag spikes. Make sure that you're using the [JVM Flags](#jvm-flags) above and that they are configured correctly (note the adjusted values when using more than `12G` of RAM).

  Using *too much RAM* can make the server perform worse because the GC has more work to do! For a vanilla server with 10 or so players, 6-10GB is a reasonable range.
