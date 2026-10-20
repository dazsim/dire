package com.workshopcraft.dire.gas;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.dire.DireConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiConsumer;

public class GasBlock extends Block {
    public static final MapCodec<GasBlock> CODEC = simpleCodec(GasBlock::new);
    public static final int MAX_LEVEL = 8;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, MAX_LEVEL);
    public static final EnumProperty<GasPhase> PHASE = EnumProperty.create("phase", GasPhase.class);
    private static final int COLOR = 0x93AE5A;

    public GasBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 1).setValue(PHASE, GasPhase.IDLE));
    }

    @Override
    public MapCodec<GasBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, PHASE);
    }

    public BlockState withLevel(int level) {
        return defaultBlockState().setValue(LEVEL, level);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return neighbour.is(this) || super.skipRendering(state, neighbour, direction);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level instanceof ServerLevel server) Gas.onPlaced(server, pos, state, oldState);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Gas.tick(level, pos, state, random);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(PHASE) == GasPhase.IDLE && DireConfig.explosionsEnabled) {
            Gas.Flame flame = Gas.flame(neighbourState);
            if (flame == Gas.Flame.FIRE || (flame == Gas.Flame.TORCH && DireConfig.torchesIgnite)) {
                return state.setValue(PHASE, GasPhase.FUSE);
            }
        }
        return state;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
        if (!(level instanceof ServerLevel server)) return;
        if (entity.isOnFire() && DireConfig.explosionsEnabled && state.getValue(PHASE) == GasPhase.IDLE) {
            Gas.startFuse(server, pos, state);
        }
        if (entity instanceof LivingEntity living && (living.tickCount + living.getId()) % 20 == 0) {
            Gas.hurt(server, living, state.getValue(LEVEL));
        }
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        if (DireConfig.explosionsEnabled) {
            if (state.getValue(PHASE) != GasPhase.BURN) level.setBlock(pos, state.setValue(PHASE, GasPhase.BURN), Block.UPDATE_CLIENTS);
        } else {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int lvl = state.getValue(LEVEL);
        if (random.nextInt(24) <= lvl) {
            level.addParticle(new DustParticleOptions(COLOR, 0.5F + lvl * 0.06F),
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
        if (state.getValue(PHASE) == GasPhase.FUSE && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
    }
}
