package com.workshopcraft.dire.neoforge;

import com.workshopcraft.dire.Dire;
import com.workshopcraft.dire.client.GasWispParticle;
import com.workshopcraft.dire.registry.DireContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = Dire.MOD_ID, dist = Dist.CLIENT)
public class DireNeoForgeClient {
    public DireNeoForgeClient(IEventBus modBus) {
        modBus.addListener((RegisterParticleProvidersEvent e) -> e.registerSpriteSet(DireContent.GAS_WISP, GasWispParticle.Provider::new));
    }
}
