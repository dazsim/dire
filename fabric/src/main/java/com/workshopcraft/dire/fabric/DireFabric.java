package com.workshopcraft.dire.fabric;

import com.workshopcraft.dire.Dire;
import net.fabricmc.api.ModInitializer;

public class DireFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Dire.init();
    }
}
