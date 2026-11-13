# GeyserNametagFix (Geyser extension)

Keeps the nametags of players that carry a passenger visible for Bedrock players - for example a cosmetics plugin's
invisible armor stand carrying a backpack.

## Why
Geyser hides the nametag of every living entity with a passenger (`LivingEntity#updateNametag`, Java hides it for
mobs). A player wearing a cosmetic that rides them as an invisible armor stand therefore has no name on Bedrock, and
after a world change Geyser even keeps the old passengers of a re-spawned player, so every name disappears.

## How
After three of Geyser's Java packet translators - passengers, teams and entity spawn - the extension puts back the
name Geyser would show without the passenger: the team's prefix, suffix and colour and its visibility rules
(`HIDE_FOR_OTHER_TEAMS`, `NEVER`, ...). It writes through Geyser's own `setNametag`, so Geyser's state matches the
client. The passenger stays a real rider on Bedrock, so it moves with the player without delay. No timer, no extra
entities, no config; nothing changes for Java players.

## Versions
The extension's version is the Geyser API version it is built for (`GeyserNametagFix-2.12.0.jar` = Geyser 2.12).
Always use the latest GeyserNametagFix version that matches the Geyser you run: it uses Geyser internals, so a build
for another Geyser version may not work.

## Build
    ./gradlew build
Output: `build/libs/GeyserNametagFix-2.12.0.jar` -> `plugins/Geyser-<platform>/extensions/`. For another Geyser
version, set the version in `build.gradle.kts` (`version` and both `compileOnly` dependencies) and `api`/`version` in
`src/main/resources/extension.yml`.

## Geyser updates
Written against Geyser 2.12 internals: `Entity#setNametag(String, boolean)`, `Team#displayName`/`isVisibleFor`,
`EntityCache`, `Registries.JAVA_PACKET_TRANSLATORS`. After a Geyser update, check the console at startup:
"GeyserNametagFix enabled" means everything matched; a warning or "disabled" means the extension needs an update -
Geyser then works as without it (names hidden again), never broken.
