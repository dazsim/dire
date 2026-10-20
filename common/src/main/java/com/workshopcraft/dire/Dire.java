package com.workshopcraft.dire;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class Dire {
    public static final String MOD_ID = "dire";
    public static final Logger LOG = LoggerFactory.getLogger("Dire");

    private Dire() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(Path configDir) {
        DireConfig.load(configDir);
        LOG.info("Dire loaded");
    }
}
