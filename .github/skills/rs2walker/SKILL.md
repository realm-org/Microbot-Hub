---
name: rs2walker
description: "Walking, navigation, and tile visibility patterns for Microbot Hub scripts. USE FOR: choosing between walkTo/walkFastCanvas/walkCanvas/walkMiniMap, checking tile visibility with Rs2Camera.isTileOnScreen, ensuring NPCs and game objects are on-screen before interaction, camera adjustments, and canvas vs walker decision logic. DO NOT USE FOR: creating new plugins from scratch, build issues, publishing."
---

# Rs2Walker & Tile Visibility Skill

Guidelines for walking, navigation, and on-screen tile validation in Microbot Hub scripts.

## Walking Methods Overview

Microbot provides several walking methods with different behaviors and use cases:

| Method | How it works | When to use | Distance |
|--------|-------------|-------------|----------|
| `Rs2Walker.walkTo(WorldPoint)` | Full web walker with pathfinding | Long distances, cross-region travel | Any |
| `Rs2Walker.walkTo(WorldPoint, distance)` | Web walker, stops within `distance` tiles | Long distances with tolerance | Any |
| `Rs2Walker.walkFastCanvas(WorldPoint)` | Clicks directly on the game canvas tile | Short distances, tile must be visible | ≤ ~12 tiles |
| `Rs2Walker.walkCanvas(WorldPoint)` | Clicks on canvas, slower than walkFast | Short distances, tile must be visible | ≤ ~12 tiles |
| `Rs2Walker.walkMiniMap(WorldPoint)` | Clicks on minimap to walk | Medium distances, avoids pathing | ~5–15 tiles |
| `Rs2Walker.walkFastLocal(LocalPoint)` | Canvas click using a LocalPoint directly | When you already have a LocalPoint | ≤ ~12 tiles |

### Key Differences

- **`walkTo`**: Uses the full web walker with API pathfinding. Reliable over any distance but slower and more CPU-intensive. Best for traveling between areas. Always works regardless of camera angle.
- **`walkFastCanvas`**: Clicks directly on the game canvas at the target tile. Very fast and lightweight, but **the tile must be visible on-screen**. Fails silently if the tile is off-screen or behind camera.
- **`walkCanvas`**: Similar to `walkFastCanvas` but with additional movement handling. Same visibility requirement.
- **`walkMiniMap`**: Clicks on the minimap. Good middle ground—doesn't require the tile to be on-screen but has limited range (~15 tiles). Useful when the target is nearby but camera isn't facing it.

## Tile Visibility Check: `Rs2Camera.isTileOnScreen`

Before using canvas-based walking or before interacting with NPCs/game objects, always verify the target is actually visible on-screen.

### Method Overloads

`Rs2Camera.isTileOnScreen` accepts:

- `LocalPoint` — most common, convert from WorldPoint if needed
- `TileObject` — for game objects directly (agility obstacles, furnaces, etc.)

### Converting WorldPoint to LocalPoint

```java
import net.runelite.api.coords.LocalPoint;

LocalPoint localTile = LocalPoint.fromWorld(
    Microbot.getClient().getTopLevelWorldView(),
    worldPoint
);
// localTile will be null if the world point is not in the loaded scene
```

**Important**: Always null-check the result. `LocalPoint.fromWorld()` returns `null` if the world point is outside the currently loaded scene (too far away).

### Basic Visibility Check Pattern

```java
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.api.coords.LocalPoint;

LocalPoint localTile = LocalPoint.fromWorld(
    Microbot.getClient().getTopLevelWorldView(), targetWorldPoint
);
if (localTile != null && Rs2Camera.isTileOnScreen(localTile)) {
    // Tile is visible — safe to use canvas walking or interact
} else {
    // Tile is NOT visible — use walkTo, walkMiniMap, or turnTo first
}
```

## Pattern 1: Canvas Walk with Visibility Fallback

Use this when walking short-to-medium distances. Prefer canvas walking when the tile is visible and close, fall back to the web walker otherwise.

```java
WorldPoint target = someWorldPoint;
int distance = Rs2Player.getWorldLocation().distanceTo(target);

if (distance <= 12) {
    LocalPoint localTile = LocalPoint.fromWorld(
        Microbot.getClient().getTopLevelWorldView(), target
    );
    if (localTile != null && Rs2Camera.isTileOnScreen(localTile)) {
        Rs2Walker.walkFastCanvas(target);
        Rs2Player.waitForWalking();
    } else {
        Rs2Walker.walkTo(target);
        Rs2Player.waitForWalking();
    }
} else {
    Rs2Walker.walkTo(target);
    Rs2Player.waitForWalking();
}
```

**Why 12 tiles?** The game canvas renders roughly 12–14 tiles in any direction depending on camera zoom. Using 12 guarantees the tile is within render range. Using a random threshold (e.g., `Rs2Random.between(7,14)`) causes the decision to flip-flop across iterations at boundary distances — always use a fixed threshold.

### Two-Phase Approach (Long then Short)

For precise tile targeting (e.g., safe spots), use the web walker for long-range approach, then canvas for exact placement:

```java
// Phase 1: Long-range approach with web walker
if (Rs2Player.distanceTo(target) > 15) {
    Rs2Walker.walkTo(target, 0);
    sleepUntil(() -> Rs2Player.distanceTo(target) <= 5, 30000);
}

// Phase 2: Precise canvas click for exact tile
if (Rs2Player.distanceTo(target) > 0) {
    Rs2Walker.walkFastCanvas(target);
    sleepUntil(() -> Rs2Player.getWorldLocation().equals(target), 15000);
}
```

## Pattern 2: Ensure NPC/Object is On-Screen Before Interaction

**Always check visibility before interacting with NPCs or game objects.** If the target is not on-screen, turn the camera to face it first. This prevents failed interactions and looks more human.

### NPCs

```java
Rs2NpcModel npc = rs2NpcCache.query().withName("Goblin").nearest();
if (npc != null) {
    if (!Rs2Camera.isTileOnScreen(npc.getLocalLocation())) {
        Rs2Camera.turnTo(npc);
    }
    Rs2Npc.interact(npc, "Attack");
}
```

For NPCs, the `Rs2Npc.validateInteractable()` helper combines the visibility check and camera turn in one call:

```java
import static net.runelite.client.plugins.microbot.util.npc.Rs2Npc.validateInteractable;

if (!Rs2Camera.isTileOnScreen(fishingspot.getLocalLocation())) {
    validateInteractable(fishingspot);
}
Rs2Npc.interact(fishingspot);
```

### Game Objects

```java
TileObject furnace = Rs2GameObject.findObjectById(furnaceId);
if (furnace == null) {
    Rs2Walker.walkTo(furnaceLocation);
    return;
}

if (!Rs2Camera.isTileOnScreen(furnace.getLocalLocation())) {
    Rs2Camera.turnTo(furnace.getLocalLocation());
    return; // Wait for camera turn to complete before clicking
}

Rs2GameObject.interact(furnace, "Smelt");
```

### TileObjectModel (Cache API)

When using `Rs2TileObjectCache`, convert the world location to a LocalPoint:

```java
Rs2TileObjectModel tree = rs2TileObjectCache.query().withName("Oak").nearest();
if (tree != null) {
    LocalPoint local = LocalPoint.fromWorld(
        Microbot.getClient().getTopLevelWorldView(), tree.getWorldLocation()
    );
    if (local != null && !Rs2Camera.isTileOnScreen(local)) {
        Rs2Camera.turnTo(local);
    }
    tree.click("Chop down");
}
```

## Pattern 3: Camera Turn for Off-Screen Targets

When a target is within interaction range but the camera isn't facing it, use `Rs2Camera.turnTo()` instead of walking:

```java
Rs2Camera.turnTo(LocalPoint localPoint);  // Turn to a tile
Rs2Camera.turnTo(Rs2NpcModel npc);        // Turn to an NPC
Rs2Camera.turnTo(TileObject gameObject);  // Turn to a game object
```

After turning the camera, either return from the loop iteration (let the next tick handle interaction) or add a brief sleep:

```java
if (!Rs2Camera.isTileOnScreen(target.getLocalLocation())) {
    Rs2Camera.turnTo(target.getLocalLocation());
    return; // Let the next iteration attempt the interaction
}
```

## Pattern 4: GPU Plugin Extended Range

When the GPU plugin is enabled, the visible range on canvas is extended. Some scripts check for this:

```java
LocalPoint localPoint = LocalPoint.fromWorld(
    Microbot.getClient().getTopLevelWorldView(), targetPoint
);
if (Rs2Camera.isTileOnScreen(localPoint) && Microbot.isPluginEnabled(GpuPlugin.class)) {
    Rs2Walker.walkFastLocal(localPoint);
} else {
    Rs2Walker.walkTo(targetPoint);
}
```

Only use this pattern if your plugin explicitly wants to take advantage of GPU draw distance. For general use, stick to the standard 12-tile threshold which works with or without GPU.

## Common Mistakes

### 1. Using canvas walking without visibility check

**Bad:**
```java
Rs2Walker.walkFastCanvas(target); // May fail silently if off-screen
```

**Good:**
```java
LocalPoint local = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
if (local != null && Rs2Camera.isTileOnScreen(local)) {
    Rs2Walker.walkFastCanvas(target);
} else {
    Rs2Walker.walkTo(target);
}
```

### 2. Randomizing the canvas vs walker distance threshold

**Bad:**
```java
// Threshold flips between canvas and walker randomly each iteration
if (distance < Rs2Random.between(7, 14)) {
    Rs2Walker.walkFastCanvas(target);
} else {
    Rs2Walker.walkTo(target);
}
```

**Good:**
```java
// Deterministic threshold — no oscillation
if (distance <= 12) {
    // ... visibility check, then canvas or fallback
} else {
    Rs2Walker.walkTo(target);
}
```

### 3. Forgetting the null check on LocalPoint.fromWorld()

**Bad:**
```java
LocalPoint local = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
Rs2Camera.isTileOnScreen(local); // NPE if target is outside loaded scene
```

**Good:**
```java
LocalPoint local = LocalPoint.fromWorld(Microbot.getClient().getTopLevelWorldView(), target);
if (local != null && Rs2Camera.isTileOnScreen(local)) {
    // safe
}
```

### 4. Interacting with an NPC/object without checking visibility

**Bad:**
```java
Rs2Npc.interact(npc, "Attack"); // Might fail if NPC is behind camera
```

**Good:**
```java
if (!Rs2Camera.isTileOnScreen(npc.getLocalLocation())) {
    Rs2Camera.turnTo(npc);
}
Rs2Npc.interact(npc, "Attack");
```

### 5. Using walkTo for very short distances

**Bad:**
```java
// Target is 3 tiles away — web walker is overkill
Rs2Walker.walkTo(target);
```

**Good:**
```java
// Target is 3 tiles away — use canvas for speed
Rs2Walker.walkFastCanvas(target);
```

### 6. Using `invoke()` for player location instead of `Rs2Player`

`Rs2Player.getWorldLocation()` is already thread-safe. Wrapping `client.getLocalPlayer().getWorldLocation()` in `invoke()` is redundant and inconsistent with other location checks that use the utility wrapper.

**Bad:**
```java
// Redundant invoke — Rs2Player already handles thread safety
int x = Microbot.getClientThread().invoke(() ->
    Microbot.getClient().getLocalPlayer().getWorldLocation().getX());
```

**Good:**
```java
// Use the thread-safe utility consistently
int x = Rs2Player.getWorldLocation().getX();
```

## Required Imports

```java
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
```

## Pattern 5: Interact with Game Object if Within Range, Otherwise Walk

When a game object (chest, sarcophagus, bank booth, etc.) is within 12–14 tiles AND on-screen, interact directly instead of walking next to it first. The game client handles pathing to the object when you click it. If it's within range but off-screen, turn the camera first. If it's farther away, walk closer before interacting.

```java
Rs2TileObjectModel chest = rs2TileObjectCache.query().withId(CHEST_ID).nearest();
if (chest != null) {
    int distance = chest.getWorldLocation().distanceTo(Rs2Player.getWorldLocation());
    if (distance <= 14) {
        // Within interaction range — ensure it's on-screen, then click
        LocalPoint chestLocal = LocalPoint.fromWorld(
            Microbot.getClient().getTopLevelWorldView(), chest.getWorldLocation()
        );
        if (chestLocal != null && !Rs2Camera.isTileOnScreen(chestLocal)) {
            Rs2Camera.turnTo(chestLocal);
            sleep(300, 600);
        }
        chest.click("Open");
        sleepUntil(() -> Rs2Player.isMoving(), Rs2Random.between(1000, 3000));
        sleepUntil(() -> !Rs2Player.isMoving(), Rs2Random.between(3000, 6000));
    } else {
        // Too far — walk closer first
        Rs2Walker.walkTo(chest.getWorldLocation());
        Rs2Player.waitForWalking();
    }
}
```

**Why 14 tiles?** The game client allows clicking objects up to ~14 tiles away if they are visible on the canvas. Using `<= 14` as the upper threshold catches cases where the object is renderable but you'd otherwise waste time walking right next to it. Use `<= 12` for more conservative scripts.

## Quick Reference: Decision Tree

```
Need to walk to a target?
├── Distance > 15 tiles?
│   └── Use Rs2Walker.walkTo()
├── Distance 5-12 tiles?
│   ├── LocalPoint.fromWorld() returns non-null AND isTileOnScreen()?
│   │   └── Use Rs2Walker.walkFastCanvas()
│   └── Otherwise?
│       └── Use Rs2Walker.walkTo() or Rs2Walker.walkMiniMap()
└── Distance < 5 tiles?
    └── Use Rs2Walker.walkFastCanvas() (almost always visible)

Need to interact with NPC/object?
├── Distance ≤ 14 tiles?
│   ├── Is it on-screen? (Rs2Camera.isTileOnScreen)
│   │   └── Yes → interact directly (game handles pathing)
│   └── No → Rs2Camera.turnTo() first, then interact
└── Distance > 14 tiles?
    └── Rs2Walker.walkTo() first, then interact when close
```
