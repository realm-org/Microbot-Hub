package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Orientation of a pitfall trap pit.
 * <p>
 * EAST_WEST = pit is 8 tiles wide along X, 2 tiles tall along Y → player jumps north↔south.
 * NORTH_SOUTH = pit is 2 tiles wide along X, 8 tiles tall along Y → player jumps east↔west.
 */
@Getter
@RequiredArgsConstructor
public enum PitOrientation {
    EAST_WEST("East-West (8x2)", 8, 2),
    NORTH_SOUTH("North-South (2x8)", 2, 8);

    private final String description;
    private final int xSize;
    private final int ySize;

    @Override
    public String toString() {
        return description;
    }
}
