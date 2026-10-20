package com.workshopcraft.dire.neoforge;

import com.workshopcraft.dire.Dire;
import com.workshopcraft.dire.gas.GasEvents;
import com.workshopcraft.dire.registry.DireContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Dire.MOD_ID)
public class DireNeoForge {
    public DireNeoForge(IEventBus modBus) {
        Dire.init(FMLPaths.CONFIGDIR.get());
        modBus.addListener(DireNeoForge::onRegister);
        modBus.addListener(DireNeoForge::onTabs);
        NeoForge.EVENT_BUS.addListener(DireNeoForge::onDeath);
        NeoForge.EVENT_BUS.addListener(DireNeoForge::onUseItem);
        NeoForge.EVENT_BUS.addListener(DireNeoForge::onUseBlock);
    }

    private static void onRegister(RegisterEvent e) {
        e.register(Registries.BLOCK, h -> DireContent.registerBlocks(h::register));
        e.register(Registries.ITEM, h -> DireContent.registerItems(h::register));
    }

    private static void onTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == DireContent.COMBAT_TAB) e.accept(DireContent.GAS_MASK);
        if (e.getTabKey() == DireContent.INGREDIENTS_TAB) e.accept(DireContent.BOTTLE_OF_GAS);
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!e.isCanceled()) GasEvents.onDeath(e.getEntity());
    }

    private static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (GasEvents.fillBottle(e.getEntity(), e.getLevel(), e.getHand())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (GasEvents.lightGas(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec())
                || GasEvents.fillBottle(e.getEntity(), e.getLevel(), e.getHand())) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
