package com.workshopcraft.dire;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class DireConfig {
    public static boolean gasEnabled = true;
    public static int zombieGasAmount = 12;
    public static int creeperGasAmount = 24;
    public static double damageScale = 1.0;
    public static boolean undeadImmune = true;
    public static int tickIntervalMin = 10;
    public static int tickIntervalMax = 20;
    public static int activeRange = 48;
    public static int maxGasPerChunk = 256;
    public static int maxChangesPerTick = 200;
    public static double decayChance = 0.03;
    public static double openSkyDecayMultiplier = 4.0;
    public static boolean explosionsEnabled = true;
    public static boolean explosionsBreakBlocks = true;
    public static double maxExplosionPower = 2.5;
    public static int maxBlastsPerTick = 4;
    public static boolean chainIgnition = true;
    public static boolean torchesIgnite = true;
    public static boolean placedTorchesIgnite = true;
    public static int fuseTicks = 30;
    public static int gasMaskWearPerSecond = 1;

    private record Entry(String key, String comment) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        e("gasEnabled", "Master switch. When false no new gas is released and existing gas fades away.");
        e("zombieGasAmount", "Total gas strength released when a zombie or zombie variant is killed.");
        e("creeperGasAmount", "Total gas strength released when a creeper is killed. Exploding creepers release none.");
        e("damageScale", "Multiplier for gas damage.");
        e("undeadImmune", "Undead mobs take no gas damage.");
        e("tickIntervalMin", "Minimum ticks between gas updates for one block.");
        e("tickIntervalMax", "Maximum ticks between gas updates for one block.");
        e("activeRange", "Gas only moves and fades when a player is within this many blocks.");
        e("maxGasPerChunk", "Gas never spreads into new blocks past this many gas blocks in one chunk.");
        e("maxChangesPerTick", "Global cap on gas block changes per server tick, per dimension.");
        e("decayChance", "Chance per update that a gas block loses one strength.");
        e("openSkyDecayMultiplier", "Decay chance multiplier for gas that can see the sky.");
        e("explosionsEnabled", "Gas ignites and explodes when it meets a flame.");
        e("explosionsBreakBlocks", "Gas blasts break blocks. Also needs the mobGriefing gamerule.");
        e("maxExplosionPower", "Power of the strongest single gas blast. A creeper is 3.");
        e("maxBlastsPerTick", "Cap on gas blasts per server tick, per dimension. The rest wait for later ticks.");
        e("chainIgnition", "Burning gas sets off the gas next to it, so a whole cloud goes up. When false only the gas touching the flame burns.");
        e("torchesIgnite", "Placing a torch or other open flame in or next to gas ignites it.");
        e("placedTorchesIgnite", "Gas that reaches an already placed torch or other open flame ignites.");
        e("fuseTicks", "Ticks of creeper hiss before gas touched by a flame ignites.");
        e("gasMaskWearPerSecond", "Durability a gas mask loses per second spent in gas.");
    }

    private static void e(String key, String comment) {
        ENTRIES.add(new Entry(key, comment));
    }

    private DireConfig() {}

    public static void load(Path dir) {
        Path file = dir.resolve(Dire.MOD_ID + ".properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                p.load(r);
            } catch (IOException ex) {
                Dire.LOG.warn("Could not read {}, using defaults", file, ex);
            }
        }
        for (Entry en : ENTRIES) {
            String v = p.getProperty(en.key);
            if (v != null) set(en.key, v.trim());
        }
        if (tickIntervalMin < 1) tickIntervalMin = 1;
        if (tickIntervalMax < tickIntervalMin) tickIntervalMax = tickIntervalMin;
        if (fuseTicks < 1) fuseTicks = 1;
        if (maxBlastsPerTick < 1) maxBlastsPerTick = 1;
        if (maxChangesPerTick < 1) maxChangesPerTick = 1;
        save(file);
    }

    private static void set(String key, String v) {
        try {
            var f = DireConfig.class.getField(key);
            Class<?> t = f.getType();
            if (t == boolean.class) f.setBoolean(null, Boolean.parseBoolean(v));
            else if (t == int.class) f.setInt(null, Integer.parseInt(v));
            else if (t == double.class) f.setDouble(null, Double.parseDouble(v));
        } catch (ReflectiveOperationException | NumberFormatException ex) {
            Dire.LOG.warn("Bad config value {}={}", key, v);
        }
    }

    private static void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write("# Dire settings. Restart the game or server after editing.\n");
                for (Entry en : ENTRIES) {
                    w.write("\n# " + en.comment + "\n" + en.key + "=" + DireConfig.class.getField(en.key).get(null) + "\n");
                }
            }
        } catch (IOException | ReflectiveOperationException ex) {
            Dire.LOG.warn("Could not write {}", file, ex);
        }
    }
}
