package com.workshopcraft.dire.gas;

import com.workshopcraft.dire.DireConfig;
import com.workshopcraft.dire.registry.DireContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class GasEvents {
    public static final int BOTTLE_AMOUNT = 4;

    private GasEvents() {}

    public static void onDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        int amount = 0;
        if (entity.is(DireContent.LARGE_GAS_SOURCES)) amount = DireConfig.creeperGasAmount;
        else if (entity.is(DireContent.SMALL_GAS_SOURCES)) amount = DireConfig.zombieGasAmount;
        if (amount > 0) Gas.release(level, BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ()), amount);
    }

    public static boolean fillBottle(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.GLASS_BOTTLE) || player.isSpectator()) return false;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = player.blockInteractionRange();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        double max = hit.getType() == HitResult.Type.MISS ? reach : hit.getLocation().distanceTo(eye);
        List<BlockPos> found = new ArrayList<>();
        for (double t = 0; t <= max; t += 0.25) {
            BlockPos p = BlockPos.containing(eye.add(look.scale(t)));
            if (!found.contains(p) && Gas.isGas(level.getBlockState(p))) found.add(p);
        }
        if (Gas.drain(level, found, BOTTLE_AMOUNT, true) < BOTTLE_AMOUNT) return false;
        if (!level.isClientSide()) {
            Gas.drain(level, found, BOTTLE_AMOUNT, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.2F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, found.getFirst());
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(DireContent.BOTTLE_OF_GAS)));
        }
        return true;
    }

    public static boolean lightGas(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        boolean flint = stack.is(Items.FLINT_AND_STEEL);
        if (!flint && !stack.is(Items.FIRE_CHARGE)) return false;
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        BlockState state = level.getBlockState(target);
        if (!Gas.isGas(state) || !DireConfig.explosionsEnabled) return false;
        if (level instanceof ServerLevel server) {
            Gas.startFuse(server, target, state);
            level.playSound(null, target, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (flint) stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            else stack.consume(1, player);
        }
        return true;
    }
}
