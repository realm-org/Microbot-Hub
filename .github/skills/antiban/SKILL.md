---
name: antiban
description: "Antiban integration patterns for Microbot Hub plugin scripts. USE FOR: adding human-like behavior to scripts, configuring Rs2Antiban and Rs2AntibanSettings, implementing mouse/camera randomization, break systems, action cooldowns, Gaussian delays, and natural timing. DO NOT USE FOR: creating new plugins from scratch, build issues, publishing."
---

# Antiban Skill

Guidelines for implementing human-like antiban behavior in Microbot Hub scripts, inspired by the MKE Wintertodt AI plugin's approach.

## Core Antiban System: Rs2Antiban

The Microbot client provides `Rs2Antiban` and `Rs2AntibanSettings` as the foundation. Always configure these in your script's initialization method.

### Setup Template

```java
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.antiban.enums.ActivityIntensity;

private void configureAntibanSettings() {
    // 1. Reset to defaults first
    Rs2Antiban.resetAntibanSettings();

    // 2. Apply a setup template that matches your activity
    //    Available templates (pick the closest match):
    //    - applyFiremakingSetup()
    //    - applyMiningSetup()
    //    - applyFishingSetup()
    //    - applyCookingSetup()
    //    - applyWoodcuttingSetup()
    //    - applyGeneralBasicSetup()
    Rs2Antiban.antibanSetupTemplates.applyFiremakingSetup();

    // 3. Set the activity type for behavior profiling
    Rs2Antiban.setActivity(Activity.GENERAL_FIREMAKING);

    // 4. Override specific settings for your script's needs
    Rs2AntibanSettings.takeMicroBreaks = false;        // Disable if using custom break system
    Rs2AntibanSettings.microBreakChance = 0.0;
    Rs2AntibanSettings.actionCooldownChance = 0.15;    // 15% chance per action
    Rs2AntibanSettings.moveMouseRandomly = true;
    Rs2AntibanSettings.moveMouseRandomlyChance = 0.36;
    Rs2AntibanSettings.moveMouseOffScreen = true;
    Rs2AntibanSettings.moveMouseOffScreenChance = 0.38;
    Rs2AntibanSettings.naturalMouse = true;
    Rs2AntibanSettings.simulateMistakes = true;
    Rs2AntibanSettings.simulateFatigue = true;

    // 5. Set activity intensity (LOW, MODERATE, HIGH)
    Rs2Antiban.setActivityIntensity(ActivityIntensity.HIGH);
}
```

### Action Cooldown Integration

After performing an action (clicking an object, using an item), conditionally trigger an action cooldown:

```java
if (Rs2AntibanSettings.usePlayStyle) {
    Rs2Antiban.actionCooldown();
}
```

Check if a cooldown is active before proceeding with logic:

```java
if (!Rs2AntibanSettings.actionCooldownActive) {
    // Safe to perform next action
}
```

### Break Handler Integration

Respect the break handler lock state in your main loop:

```java
private boolean shouldPauseForBreaks() {
    // Check Rs2Antiban cooldown
    if (Rs2AntibanSettings.actionCooldownActive) {
        return true;
    }
    // Check break handler
    if (BreakHandlerScript.isLockState()) {
        return true;
    }
    return false;
}
```

### Overlay Integration

Show antiban status in your overlay:

```java
if (config.showAntibanOverlay()
    && Rs2AntibanSettings.antibanEnabled
    && Rs2Antiban.getActivity() != null) {
    Rs2Antiban.renderAntibanOverlayComponents(panelComponent);
}
```

---

## Human-Like Timing

### Gaussian Delays (preferred over uniform random)

Use `sleepGaussian(mean, stddev)` instead of flat `sleep(random)`. Gaussian distributions create a natural bell curve where most delays cluster near the mean with occasional outliers.

```java
// Standard action delay: most delays around 1600ms, occasionally 1200-2000ms
sleepGaussian(1600, 400);

// Quick reaction delay: most around 200ms
sleepGaussian(200, 150);

// Post-action cooldown: centered on 1200ms
sleepGaussian(1200, 300);
```

### Variable Base Delays

Add randomized delays after actions using both a base and a variation:

```java
int baseDelay = 150;
int variation = 50 + random.nextInt(100);
sleepGaussian(baseDelay, variation);
```

---

## Mouse Behavior

### Mouse Nudge After Actions

After interacting with game objects, occasionally perform a small mouse wobble. This simulates natural hand jitter:

```java
private void maybeNudgeMouse() {
    // Probability: 18% base + Gaussian component (5-40% effective range)
    int moveChance = (int) Math.round(18 + Math.abs(random.nextGaussian()) * 10);
    moveChance = Math.max(5, Math.min(40, moveChance));
    if (random.nextInt(100) >= moveChance) return;

    Point start = Microbot.getMouse().getMousePosition();
    if (start == null) return;

    // First micro-movement: σ ≈ 20px, capped ±80px
    int dx = (int) Math.max(-80, Math.min(80, random.nextGaussian() * 20));
    int dy = (int) Math.max(-80, Math.min(80, random.nextGaussian() * 20));
    Microbot.getMouse().move(start.x + dx, start.y + dy);

    // 50% chance of a follow-up wobble
    if (random.nextBoolean()) {
        sleepGaussian(30, 15);
        int dx2 = (int) Math.max(-30, Math.min(30, random.nextGaussian() * 10));
        int dy2 = (int) Math.max(-30, Math.min(30, random.nextGaussian() * 10));
        Microbot.getMouse().move(start.x + dx + dx2, start.y + dy + dy2);
    }
}
```

### Mouse Offscreen During Idle

When the script is idle/waiting, move the mouse offscreen to simulate AFK:

```java
// Move mouse offscreen with 85% probability of going to a natural edge position
Rs2Antiban.moveMouseOffScreen(85);
```

### Hover Before Action

Before a game round or event starts, hover over the next interactive object slightly early (1-8 seconds before), like a real player anticipating:

```java
// Calculate random hover time once per round
int hoverBeforeStartTime = Rs2Random.randomGaussian(1000, 8000);
hoverBeforeStartTime = Math.max(500, Math.min(8000, hoverBeforeStartTime));

// When time remaining reaches hover threshold
if (timeUntilStart > 0 && timeUntilStart <= hoverBeforeStartTime) {
    Rs2GameObject.hoverOverObject(nextObject);
}
```

### Random Mouse Movements During Actions

Periodically move the mouse to simulate checking the screen:

```java
if (currentTime - lastMouseMovement > 15000 + random.nextInt(10000)) {
    if (random.nextInt(100) < 15) { // 15% chance
        if (currentBrazier != null && random.nextBoolean()) {
            Rs2GameObject.hoverOverObject(currentBrazier);
        }
        lastMouseMovement = currentTime;
    }
}
```

---

## Camera Behavior

### Natural Camera Movements

Simulate camera adjustments using keyboard holds (not instant teleports). Use Gaussian distributions for yaw/pitch deltas so small adjustments are common and large sweeps are rare:

```java
private void performHumanLikeCameraMovement() {
    // Yaw: μ=0°, σ=20° → 68% within ±20°, rarely >±60°
    int yawDelta = (int) Math.round(random.nextGaussian() * 20);
    yawDelta = Math.max(-75, Math.min(75, yawDelta));

    int targetYaw = (Rs2Camera.getAngle() + yawDelta + 360) % 360;

    // Hold arrow key proportional to rotation needed
    int degreesToMove = Math.abs(Rs2Camera.getAngleTo(targetYaw));
    int pressTime = 80 + (int) (degreesToMove * 2.2);
    pressTime = (int) (pressTime * (0.8 + random.nextGaussian() * 0.1));

    if (Rs2Camera.getAngleTo(targetYaw) > 0) {
        Rs2Keyboard.keyHold(KeyEvent.VK_LEFT);
        sleepGaussian(pressTime, (int) (pressTime * 0.15));
        Rs2Keyboard.keyRelease(KeyEvent.VK_LEFT);
    } else {
        Rs2Keyboard.keyHold(KeyEvent.VK_RIGHT);
        sleepGaussian(pressTime, (int) (pressTime * 0.15));
        Rs2Keyboard.keyRelease(KeyEvent.VK_RIGHT);
    }

    // 42% chance to also adjust pitch
    if (random.nextInt(100) < 42) {
        float deltaPct = (float) (random.nextGaussian() * 0.10);
        float targetPitchPct = Math.max(0.30f, Math.min(0.90f,
            Rs2Camera.cameraPitchPercentage() + deltaPct));
        Rs2Camera.adjustPitch(targetPitchPct);
    }
}
```

### Camera Movement Scheduling

Use a probability that increases over time since last movement, with a configurable minimum frequency:

```java
private boolean shouldPerformCameraMovement(long currentTime) {
    long cameraMovementMinDelay = config.cameraMovementFrequency() * 1000L;
    if (cameraMovementMinDelay <= 0) return false;

    long timeSinceLastMove = currentTime - lastCameraMovement;
    if (timeSinceLastMove < cameraMovementMinDelay) return false;

    // Probability increases over time
    double baseChance = 0.001;
    double timeMultiplier = Math.min(50.0, timeSinceLastMove / (double) cameraMovementMinDelay);
    return random.nextDouble() <= baseChance * timeMultiplier;
}
```

---

## Custom Break System

For scripts that need more control than the built-in break handler, implement a custom break manager with AFK and logout break types.

### Break Types

| Type | Duration | Behavior |
|------|----------|----------|
| AFK (Short) | 1-6 min | Mouse goes offscreen, player stays logged in |
| Logout (Long) | 5-40 min | Player logs out entirely |

### Key Design Principles

1. **Location-aware**: Only trigger breaks in safe locations (e.g., bank areas, safe spots)
2. **Configurable intervals**: Min/max break interval, min/max break duration
3. **Break type weighting**: Configurable chance for AFK vs logout (e.g., 60% AFK / 40% logout)
4. **Emergency override**: Always allow eating/healing even during breaks
5. **Reward-safe**: Never interrupt reward collection or banking flows
6. **Graceful timeout**: If a safe spot isn't reached within a time limit, walk to one

### Config Pattern for Breaks

```java
@ConfigSection(name = "Break System", position = 5)
String breakSection = "breaks";

@ConfigItem(section = breakSection)
default boolean enableCustomBreaks() { return true; }

@ConfigItem(section = breakSection)
default int minBreakInterval() { return 20; } // minutes

@ConfigItem(section = breakSection)
default int maxBreakInterval() { return 60; } // minutes

@ConfigItem(section = breakSection)
default int logoutBreakChance() { return 40; } // % chance for logout vs AFK
```

### Config Pattern for Advanced Antiban

```java
@ConfigSection(name = "Advanced Options", position = 6)
String advancedSection = "advanced";

@ConfigItem(section = advancedSection)
default boolean humanizedTiming() { return true; }

@ConfigItem(section = advancedSection)
default boolean randomMouseMovements() { return true; }

@ConfigItem(section = advancedSection,
    description = "Higher = less frequent. 0 to disable.")
default int cameraMovementFrequency() { return 10; } // seconds

@ConfigItem(section = advancedSection)
default boolean showAntibanOverlay() { return true; }
```

---

## Spam Clicking (Round Start Behavior)

Simulate impatient clicking when a game event is about to begin — a common real-player behavior:

```java
private boolean spamClickingActive = false;
private long spamClickEndTime = 0;
private long lastSpamClick = 0;

// Activate spam clicking in a short window before event
private void startSpamClicking(GameObject target) {
    spamClickingActive = true;
    spamClickEndTime = System.currentTimeMillis() + Rs2Random.between(800, 2500);
    spamClickTarget = target;
}

// Execute during main loop — rapidly click target with variable intervals
private void executeSpamClicking() {
    if (!spamClickingActive) return;
    if (System.currentTimeMillis() > spamClickEndTime) {
        spamClickingActive = false;
        return;
    }
    long now = System.currentTimeMillis();
    if (now - lastSpamClick > 200 + random.nextInt(300)) {
        Rs2GameObject.interact(spamClickTarget, "Use");
        lastSpamClick = now;
    }
}
```

---

## Live Action Duration Tracking

Track real action durations via XP drops and refine timing estimates using an exponential moving average (EMA). This makes the bot adapt to the player's actual performance:

```java
// Track via @Subscribe or onStatChanged
public static void onStatChanged(StatChanged event) {
    long now = System.currentTimeMillis();
    if (event.getSkill() == Skill.WOODCUTTING && lastWoodcuttingXpDropTime > 0) {
        long duration = now - lastWoodcuttingXpDropTime;
        if (duration > 600 && duration < 10000) { // sanity check
            // EMA smoothing: 80% old, 20% new
            avgChopMs = (avgChopMs * 0.8) + (duration * 0.2);
        }
    }
    lastWoodcuttingXpDropTime = now;
}
```

---

## Checklist: Adding Antiban to a Script

When updating or creating a script, apply these in order:

1. **Initialize Rs2Antiban** — Call `configureAntibanSettings()` in your `run()` method before the main loop
2. **Gaussian delays** — Replace all `sleep(random)` calls with `sleepGaussian(mean, stddev)`
3. **Action cooldowns** — Add `Rs2Antiban.actionCooldown()` after object interactions (when `usePlayStyle` is enabled)
4. **Mouse nudge** — Call `maybeNudgeMouse()` after clicking game objects
5. **Camera movements** — Schedule `performHumanLikeCameraMovement()` in the main loop with time-based probability
6. **Mouse offscreen** — Move mouse offscreen during idle/waiting periods
7. **Hover pre-action** — Hover over the next target object before the action window opens
8. **Break system** — Implement or integrate breaks (AFK + logout) with safe-location checks
9. **Overlay** — Show antiban status via `Rs2Antiban.renderAntibanOverlayComponents()`
10. **Config options** — Expose humanized timing, mouse movements, camera frequency, and overlay toggle

## Common Mistakes

- **Using uniform `random.nextInt()` for delays** — Always prefer Gaussian distributions for natural bell-curve timing
- **Instant camera teleports** — Use keyboard holds (`keyHold`/`keyRelease`) to simulate real camera panning
- **Breaking during critical flows** — Never interrupt banking, reward collection, or combat healing
- **Forgetting to reset antiban on startup** — Always call `Rs2Antiban.resetAntibanSettings()` before applying templates
- **Hard-coded delay values** — Make timing configurable or at least add ±20% Gaussian variation
- **Not checking `actionCooldownActive`** — The script will double-act if you don't gate actions behind this check
