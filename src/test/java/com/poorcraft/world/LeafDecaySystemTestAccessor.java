package com.poorcraft.world;

public final class LeafDecaySystemTestAccessor {

    private LeafDecaySystemTestAccessor() {
    }

    public static int getNearestLogDistance(LeafDecaySystem system, int x, int y, int z) {
        return system.debugGetNearestLogDistance(x, y, z);
    }
}
