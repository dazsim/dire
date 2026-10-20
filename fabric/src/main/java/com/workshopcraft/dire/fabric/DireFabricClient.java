package com.workshopcraft.dire.fabric;

import com.workshopcraft.dire.client.GasWispParticle;
import com.workshopcraft.dire.registry.DireContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

public class DireFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(DireContent.GAS_WISP, GasWispParticle.Provider::new);
    }
}
