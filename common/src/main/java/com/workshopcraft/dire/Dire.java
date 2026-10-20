package com.workshopcraft.dire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Dire {
    public static final String MOD_ID = "dire";
    public static final Logger LOG = LoggerFactory.getLogger("Dire");

    private Dire() {
    }

    public static void init() {
        LOG.info("Dire loaded");
    }
}
