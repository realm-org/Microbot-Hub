package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

/**
 * Represents a single pitfall trap location with orientation-aware positioning.
 * <p>
 * Pits are 8x2 (EAST_WEST) or 2x8 (NORTH_SOUTH). The clickable ground objects
 * ("Trap"/"Jump") occupy the middle 4 tiles.
 * <p>
 * For EAST_WEST pits: the NPC is north or south → player jumps across Y axis.
 * For NORTH_SOUTH pits: the NPC is east or west → player jumps across X axis.
 */
@Getter
@RequiredArgsConstructor
public class PitLocation {

    public enum Side {
        NORTH, SOUTH, EAST, WEST
    }

    private final WorldPoint center;
    private final PitOrientation orientation;
    private final WorldPoint groundObjectPoint;

    /**
     * Determines which side of this pit an NPC is on, based on the pit's orientation.
     * For EAST_WEST pits (8x2): compares Y coords → NORTH or SOUTH.
     * For NORTH_SOUTH pits (2x8): compares X coords → EAST or WEST.
     */
    public Side getNpcSide(WorldPoint npcLocation) {
        if (orientation == PitOrientation.EAST_WEST) {
            // Pit is wide along X, narrow along Y → NPC is north or south
            return npcLocation.getY() >= center.getY() ? Side.NORTH : Side.SOUTH;
        } else {
            // Pit is tall along Y, narrow along X → NPC is east or west
            return npcLocation.getX() >= center.getX() ? Side.EAST : Side.WEST;
        }
    }

    /**
     * Returns a WorldPoint on the given side of the pit where the player should
     * stand to tease the NPC. The tile is adjacent to the pit edge on the NPC's
     * side, roughly centered along the pit's long axis.
     */
    public WorldPoint getTeasingTile(Side npcSide) {
        int offsetDistance = 2; // tiles away from pit center toward the NPC's side
        switch (npcSide) {
            case NORTH:
                return new WorldPoint(center.getX(), center.getY() + offsetDistance, center.getPlane());
            case SOUTH:
                return new WorldPoint(center.getX(), center.getY() - offsetDistance, center.getPlane());
            case EAST:
                return new WorldPoint(center.getX() + offsetDistance, center.getY(), center.getPlane());
            case WEST:
                return new WorldPoint(center.getX() - offsetDistance, center.getY(), center.getPlane());
            default:
                return center;
        }
    }

    /**
     * Returns the distance from this pit's center to the given point.
     */
    public int distanceTo(WorldPoint point) {
        return center.distanceTo(point);
    }
}
