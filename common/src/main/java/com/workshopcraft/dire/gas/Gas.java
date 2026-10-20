package com.workshopcraft.dire.gas;

import com.workshopcraft.dire.DireConfig;
import com.workshopcraft.dire.registry.DireContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Gas {
    public enum Flame { NONE, TORCH, FIRE }

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int BURN_DELAY = 2;
    private static final int BLAST_COOLDOWN = 40;

    private static final class LevelData {
        long tick = -1;
        int changes;
        int blasts;
        final Map<Long, Long> due = new HashMap<>();
        final Map<Long, long[]> chunkCounts = new HashMap<>();
        final Map<Long, Long> blastCells = new HashMap<>();
    }

    private static final Map<ServerLevel, LevelData> DATA = new WeakHashMap<>();

    private Gas() {}

    private static LevelData data(ServerLevel level) {
        LevelData d = DATA.computeIfAbsent(level, l -> new LevelData());
        long now = level.getGameTime();
        if (d.tick != now) {
            d.tick = now;
            d.changes = 0;
            d.blasts = 0;
        }
        return d;
    }

    public static boolean isGas(BlockState state) {
        return state.is(DireContent.GAS);
    }

    public static Flame flame(BlockState state) {
        if (state.getBlock() instanceof BaseFireBlock) return state.is(DireContent.IGNITES_GAS) ? Flame.FIRE : Flame.NONE;
        if (state.getFluidState().is(FluidTags.LAVA)) return Flame.FIRE;
        if (!state.is(DireContent.IGNITES_GAS)) return Flame.NONE;
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return Flame.NONE;
        return Flame.TORCH;
    }

    private static void schedule(ServerLevel level, BlockPos pos, RandomSource random, int factor) {
        int min = DireConfig.tickIntervalMin, max = DireConfig.tickIntervalMax;
        level.scheduleTick(pos, DireContent.GAS, (min + random.nextInt(max - min + 1)) * factor);
    }

    static void onPlaced(ServerLevel level, BlockPos pos, BlockState state, BlockState old) {
        GasPhase phase = state.getValue(GasBlock.PHASE);
        boolean changed = !isGas(old) || old.getValue(GasBlock.PHASE) != phase;
        LevelData d = data(level);
        if (phase == GasPhase.FUSE && changed) {
            level.playSound(null, pos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1.0F, 0.9F);
            d.due.put(pos.asLong(), level.getGameTime() + DireConfig.fuseTicks);
            level.scheduleTick(pos, DireContent.GAS, DireConfig.fuseTicks);
        } else if (phase == GasPhase.BURN && changed) {
            d.due.put(pos.asLong(), level.getGameTime() + BURN_DELAY);
            level.scheduleTick(pos, DireContent.GAS, BURN_DELAY);
        } else if (!level.getBlockTicks().hasScheduledTick(pos, DireContent.GAS)) {
            schedule(level, pos, level.getRandom(), 1);
        }
    }

    public static void startFuse(ServerLevel level, BlockPos pos, BlockState state) {
        if (!DireConfig.explosionsEnabled || state.getValue(GasBlock.PHASE) != GasPhase.IDLE) return;
        level.setBlock(pos, state.setValue(GasBlock.PHASE, GasPhase.FUSE), Block.UPDATE_CLIENTS);
    }

    static void tick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        LevelData d = data(level);
        GasPhase phase = state.getValue(GasBlock.PHASE);
        if (phase != GasPhase.IDLE) {
            Long due = d.due.get(pos.asLong());
            long now = level.getGameTime();
            if (due != null && due > now) {
                level.scheduleTick(pos, DireContent.GAS, (int) (due - now));
                return;
            }
            if (burn(level, pos, state, d)) d.due.remove(pos.asLong());
            return;
        }
        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, DireConfig.activeRange)) {
            schedule(level, pos, random, 4);
            return;
        }
        if (d.changes >= DireConfig.maxChangesPerTick) {
            level.scheduleTick(pos, DireContent.GAS, 1 + random.nextInt(5));
            return;
        }
        if (DireConfig.explosionsEnabled) {
            for (Direction dir : Direction.values()) {
                Flame f = flame(level.getBlockState(pos.relative(dir)));
                if (f == Flame.FIRE || (f == Flame.TORCH && DireConfig.placedTorchesIgnite)) {
                    startFuse(level, pos, state);
                    return;
                }
            }
        }

        int start = state.getValue(GasBlock.LEVEL);
        int lvl = start;
        double decay = DireConfig.decayChance * (level.canSeeSky(pos) ? DireConfig.openSkyDecayMultiplier : 1.0);
        if (!DireConfig.gasEnabled) decay = Math.max(decay, 0.25);
        if (random.nextDouble() < decay) lvl--;

        if (lvl > 0) {
            BlockPos below = pos.below();
            BlockState bs = level.getBlockState(below);
            if (bs.isAir()) {
                level.setBlock(below, DireContent.GAS.withLevel(lvl), Block.UPDATE_CLIENTS);
                d.changes++;
                lvl = 0;
            } else if (isIdleGas(bs) && bs.getValue(GasBlock.LEVEL) < GasBlock.MAX_LEVEL) {
                int move = Math.min(lvl, GasBlock.MAX_LEVEL - bs.getValue(GasBlock.LEVEL));
                level.setBlock(below, bs.setValue(GasBlock.LEVEL, bs.getValue(GasBlock.LEVEL) + move), Block.UPDATE_CLIENTS);
                d.changes++;
                lvl -= move;
            }
        }

        if (lvl > 0) {
            int first = random.nextInt(4);
            for (int i = 0; i < 4 && lvl > 0; i++) {
                BlockPos np = pos.relative(HORIZONTAL[(first + i) % 4]);
                if (!level.isLoaded(np)) continue;
                BlockState ns = level.getBlockState(np);
                int other;
                if (ns.isAir()) other = 0;
                else if (isIdleGas(ns)) other = ns.getValue(GasBlock.LEVEL);
                else continue;
                int move = (lvl - other) / 2;
                if (move <= 0 && other == 0 && level.getBlockState(np.below()).isAir()) move = lvl;
                if (move <= 0) continue;
                if (other == 0) {
                    if (!roomInChunk(level, np, d)) continue;
                    level.setBlock(np, DireContent.GAS.withLevel(move), Block.UPDATE_CLIENTS);
                } else {
                    level.setBlock(np, ns.setValue(GasBlock.LEVEL, other + move), Block.UPDATE_CLIENTS);
                }
                d.changes++;
                lvl -= move;
            }
        }

        if (lvl <= 0) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            d.changes++;
        } else if (lvl != start) {
            level.setBlock(pos, state.setValue(GasBlock.LEVEL, lvl), Block.UPDATE_CLIENTS);
            d.changes++;
            schedule(level, pos, random, 1);
        } else {
            schedule(level, pos, random, 2);
        }
    }

    private static boolean isIdleGas(BlockState state) {
        return isGas(state) && state.getValue(GasBlock.PHASE) == GasPhase.IDLE;
    }

    private static boolean roomInChunk(ServerLevel level, BlockPos pos, LevelData d) {
        long key = ChunkPos.pack(pos);
        long now = level.getGameTime();
        long[] entry = d.chunkCounts.get(key);
        if (entry == null || now - entry[1] > 100) {
            if (d.chunkCounts.size() > 4096) d.chunkCounts.clear();
            entry = new long[]{countGas(level.getChunkAt(pos)), now};
            d.chunkCounts.put(key, entry);
        }
        if (entry[0] >= DireConfig.maxGasPerChunk) return false;
        entry[0]++;
        return true;
    }

    private static int countGas(LevelChunk chunk) {
        int n = 0;
        for (LevelChunkSection s : chunk.getSections()) {
            if (s.hasOnlyAir() || !s.maybeHas(Gas::isGas)) continue;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++)
                        if (isGas(s.getBlockState(x, y, z))) n++;
        }
        return n;
    }

    private static boolean burn(ServerLevel level, BlockPos pos, BlockState state, LevelData d) {
        int lvl = state.getValue(GasBlock.LEVEL);
        long now = level.getGameTime();
        long cell = BlockPos.asLong(pos.getX() >> 1, pos.getY() >> 1, pos.getZ() >> 1);
        Long last = d.blastCells.get(cell);
        boolean blast = last == null || now - last > BLAST_COOLDOWN;
        if (blast && d.blasts >= DireConfig.maxBlastsPerTick) {
            level.scheduleTick(pos, DireContent.GAS, 1);
            return false;
        }
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 3 + lvl, 0.35, 0.35, 0.35, 0.02);
        if (DireConfig.chainIgnition) {
            for (Direction dir : Direction.values()) {
                BlockPos np = pos.relative(dir);
                if (!level.isLoaded(np)) continue;
                BlockState ns = level.getBlockState(np);
                if (isGas(ns) && ns.getValue(GasBlock.PHASE) != GasPhase.BURN) {
                    level.setBlock(np, ns.setValue(GasBlock.PHASE, GasPhase.BURN), Block.UPDATE_CLIENTS);
                }
            }
        }
        if (blast) {
            d.blasts++;
            if (d.blastCells.size() > 2048) d.blastCells.values().removeIf(t -> now - t > 100);
            d.blastCells.put(cell, now);
            float power = (float) Math.min(DireConfig.maxExplosionPower, 1.0 + lvl * 0.2);
            Level.ExplosionInteraction mode = DireConfig.explosionsBreakBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
            level.explode(null, x, y, z, power, mode);
        } else if (level.getRandom().nextInt(3) == 0) {
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1.2F + level.getRandom().nextFloat() * 0.4F);
        }
        return true;
    }

    public static void hurt(ServerLevel level, LivingEntity entity, int lvl) {
        if (!entity.isAlive()) return;
        if (DireConfig.undeadImmune && entity.is(EntityTypeTags.UNDEAD)) return;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(DireContent.GAS_MASK)) {
            if (DireConfig.gasMaskWearPerSecond > 0) head.hurtAndBreak(DireConfig.gasMaskWearPerSecond, entity, EquipmentSlot.HEAD);
            return;
        }
        float amount = (float) (Math.ceil(lvl * 0.75) * DireConfig.damageScale);
        if (amount <= 0) return;
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DireContent.GAS_DAMAGE));
        entity.hurtServer(level, source, amount);
    }

    public static void release(ServerLevel level, BlockPos origin, int amount) {
        if (!DireConfig.gasEnabled || amount <= 0) return;
        LevelData d = data(level);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        BlockPos start = origin;
        for (int i = 0; i < 3 && !canHold(level.getBlockState(start)); i++) start = start.above();
        queue.add(start);
        seen.add(start);
        while (amount > 0 && !queue.isEmpty() && seen.size() < 128) {
            BlockPos p = queue.poll();
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!canHold(s)) continue;
            int cur = isGas(s) ? s.getValue(GasBlock.LEVEL) : 0;
            int add = Math.min(GasBlock.MAX_LEVEL - cur, amount);
            if (add > 0 && (cur > 0 || roomInChunk(level, p, d))) {
                level.setBlock(p, isGas(s) ? s.setValue(GasBlock.LEVEL, cur + add) : DireContent.GAS.withLevel(add), Block.UPDATE_CLIENTS);
                amount -= add;
            }
            List<Direction> dirs = new ArrayList<>(List.of(Direction.values()));
            java.util.Collections.shuffle(dirs, new java.util.Random(level.getRandom().nextLong()));
            for (Direction dir : dirs) {
                BlockPos n = p.relative(dir);
                if (seen.add(n)) queue.add(n);
            }
        }
    }

    private static boolean canHold(BlockState s) {
        return s.isAir() || isIdleGas(s);
    }

    public static int drain(Level level, List<BlockPos> positions, int wanted, boolean simulate) {
        int got = 0;
        for (BlockPos p : positions) {
            if (got >= wanted) break;
            BlockState s = level.getBlockState(p);
            if (!isIdleGas(s)) continue;
            int lvl = s.getValue(GasBlock.LEVEL);
            int take = Math.min(lvl, wanted - got);
            got += take;
            if (!simulate) {
                level.setBlock(p, lvl - take <= 0 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                        : s.setValue(GasBlock.LEVEL, lvl - take), Block.UPDATE_ALL);
            }
        }
        return got;
    }
}
