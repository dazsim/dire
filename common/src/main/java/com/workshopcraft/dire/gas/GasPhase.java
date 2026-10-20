package com.workshopcraft.dire.gas;

import net.minecraft.util.StringRepresentable;

public enum GasPhase implements StringRepresentable {
    IDLE("idle"),
    FUSE("fuse"),
    BURN("burn");

    private final String name;

    GasPhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
