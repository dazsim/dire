package com.workshopcraft.dire.fabric;

import com.workshopcraft.dire.Dire;
import com.workshopcraft.dire.gas.GasEvents;
import com.workshopcraft.dire.registry.DireContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;

public class DireFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Dire.init(FabricLoader.getInstance().getConfigDir());
        DireContent.registerAllDirect();
        CreativeModeTabEvents.modifyOutputEvent(DireContent.COMBAT_TAB).register(e -> e.accept(DireContent.GAS_MASK));
        CreativeModeTabEvents.modifyOutputEvent(DireContent.INGREDIENTS_TAB).register(e -> e.accept(DireContent.BOTTLE_OF_GAS));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> GasEvents.onDeath(entity));
        UseItemCallback.EVENT.register((player, level, hand) ->
                GasEvents.fillBottle(player, level, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS);
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                GasEvents.lightGas(player, level, hand, hit) || GasEvents.fillBottle(player, level, hand) ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }
}
