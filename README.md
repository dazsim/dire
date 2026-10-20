# Dire

A green toxic gas for Minecraft. It hurts anything standing in it, sinks into low ground and goes up with a bang if it meets a flame.

Minecraft 26.1 to 26.1.x, NeoForge and Fabric (Fabric needs Fabric API).

## Gas

- Killing a zombie (or any zombie variant) leaves a small puff of gas. Killing a creeper leaves a bigger one. A creeper that explodes uses up its gas and leaves none.
- Gas has 8 strength levels. It sinks first, then spreads sideways into weaker gas and looks a few blocks around for a drop to pour into, so it collects in pits and caves.
- It fades slowly, faster under open sky. Water removes it.
- You can see it as a faint green haze with a few drifting green specks.
- Standing in gas hurts, more at higher strength. It can kill. All undead are immune. Creative and spectator players are safe.
- Gas only moves and fades when a player is within 48 blocks.

## Fire

Torches, wall torches, copper torches, campfires, fire, lava, lit furnaces, smokers and blast furnaces, flint and steel, fire charges, burning arrows and anything on fire set gas off. Lanterns, soul torches, soul lanterns and soul campfires are safe.

A flame in gas hisses like a creeper for about 1.5 seconds, then the gas burns. The burn spreads block to block through the whole cloud as a wave of small creeper-like blasts, so a big pool covers more ground and does more damage than a small one. Blasts break blocks unless the mobGriefing gamerule is off or block breaking is turned off in the config. TNT and other explosions that break blocks also set gas off.

## Items

- Bottle of Gas: use an empty glass bottle on gas (strength 4 or more along your line of sight).
- Gas Mask: worn in the helmet slot, stops gas damage and wears down while you are in gas. Repair with leather.

Gas mask recipe:

```
L L L
L   L
N F N
```

L = leather, N = iron nugget, F = charcoal or dried kelp.

## Config

`config/dire.properties` is written on first start, with a comment for every setting. It covers the gas amounts, damage, update rates, range and caps (per chunk, per tick, blasts per tick), decay, explosions, block breaking, torch ignition (`torchesIgnite`, `placedTorchesIgnite`) and gas mask wear.

The block tag `dire:ignites_gas` lists the flames that set gas off, and the entity tags `dire:releases_small_gas` and `dire:releases_large_gas` list the mobs that leave gas behind.

## Building

`./gradlew build`. Jars end up in `neoforge/build/libs` and `fabric/build/libs`.
