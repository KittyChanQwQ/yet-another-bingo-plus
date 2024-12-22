# Overview

The Yet Another Bingo API is intended for other mods to collect info about the current game, team scores, and player statistics.

It *does not* provide an interface for defining custom objectives or objective types, although this may be added in the future.

## Add the GitLab Maven repository

To add the API dependency to your Fabric mod, add the following maven repository to your project.

::: code-group

```groovy [build.gradle]
repositories {
	maven {
		name = "Yet Another Bingo"
		url = "https://gitlab.com/api/v4/projects/48318565/packages/maven"
		content {
			includeGroup("me.jfenn.bingo")
		}
	}
}
```

:::

## Add the dependency

In the `dependencies` section, add the following dependency. Make sure `yet_another_bingo_version` is set to the API version you want to target - which can be found [on the GitLab package registry](https://gitlab.com/horrific-tweaks/bingo/-/packages).

::: code-group

```groovy [build.gradle]
dependencies {
	modImplementation "me.jfenn.bingo:api:${yet_another_bingo_version}"
}
```

:::

## Use the API

The below methods may be used to obtain game and player details:

- `BingoApi.getGame()`
- `BingoApi.getTeams()`
- `BingoApi.getPlayerStats(UUID)`

If the bingo mod is not yet initialized, these functions may throw `IllegalStateException`.

If the bingo mod is not installed at all, any usage of `BingoApi` will throw a [`NoClassDefFoundError`](https://docs.oracle.com/javase/8/docs/api/java/lang/NoClassDefFoundError.html). You can work around this by either adding the mod as a required dependency in `fabric.mod.json`, or by checking if the mod is loaded with `FabricLoader.getInstance().isModLoaded("yet-another-minecraft-bingo-api")`.
