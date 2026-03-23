package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum PitFallTrapHunting {

    // TODO: Replace placeholder IDs with actual ObjectIDs, NpcIDs, and ItemIDs from in-game dev tools
    // Pit locations: center, orientation, and ground object point per pit (up to 3 per creature)
    SUNLIGHT_ANTELOPE(
            "Sunlight Antelope",
            "Sunlight antelope",
            -1,     // npcId — TODO: fill from game
            -1,     // pitObjectId — unset pit ground object (interact "Trap")
            -1,     // spikedPitObjectId — set pit (interact "Jump")
            -1,     // fullPitObjectId — pit with caught creature (interact "Check")
            -1,     // emptyPitObjectId — pit after loot collected (collapsed/reset state)
            72,     // requiredHunterLevel
            new WorldPoint(1600, 3000, 0), // huntingPoint — TODO: fill from game
            List.of(
                    new PitLocation(
                            new WorldPoint(1600, 3002, 0),  // pit 1 center — TODO
                            PitOrientation.EAST_WEST,
                            new WorldPoint(1600, 3002, 0)   // pit 1 ground object — TODO
                    ),
                    new PitLocation(
                            new WorldPoint(1604, 3002, 0),  // pit 2 center — TODO
                            PitOrientation.EAST_WEST,
                            new WorldPoint(1604, 3002, 0)   // pit 2 ground object — TODO
                    ),
                    new PitLocation(
                            new WorldPoint(1608, 3002, 0),  // pit 3 center — TODO
                            PitOrientation.NORTH_SOUTH,
                            new WorldPoint(1608, 3002, 0)   // pit 3 ground object — TODO
                    )
            ),
            List.of(-1, -1),   // itemsToDrop — TODO: fill with big bones ID, fur ID etc.
            -1,                 // furItemId — TODO: antelope fur item ID
            -1,                 // antlerItemId — TODO: sunlight antler item ID
            -1                  // lootId — raw meat for banking (ItemID.HUNTING_ANTELOPESUN_MEAT)
    );

    private final String name;
    private final String npcName;
    private final int npcId;
    private final int pitObjectId;
    private final int spikedPitObjectId;
    private final int fullPitObjectId;
    private final int emptyPitObjectId;
    private final int requiredHunterLevel;
    private final WorldPoint huntingPoint;
    private final List<PitLocation> pitLocations;
    private final List<Integer> itemsToDrop;
    private final int furItemId;
    private final int antlerItemId;
    private final Integer lootId;

    /**
     * Find the pit location closest to the given world point.
     */
    public PitLocation findNearestPit(WorldPoint point) {
        PitLocation nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        for (PitLocation pit : pitLocations) {
            int dist = pit.distanceTo(point);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pit;
            }
        }
        return nearest;
    }

    @Override
    public String toString() {
        return name;
    }
}
