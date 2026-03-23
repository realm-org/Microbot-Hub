package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

public class PitFallTrapInventoryHandlerScript extends Script {
    private PitFallTrapHunterConfig config;
    private PitFallTrapHunterScript script;

    public boolean run(PitFallTrapHunterConfig config, PitFallTrapHunterScript script) {
        this.config = config;
        this.script = script;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (!this.isRunning()) return;
                if (!script.isRunning()) return;
                if (!script.forceDrop) {
                    if (Rs2Player.isMoving()) return;
                    if (Rs2Player.isAnimating()) return;
                    if (Rs2Inventory.count() > Rs2Random.between(8, 28)) {
                        cleanInventory();
                    }
                    return;
                }
                cleanInventory();
            } catch (Exception ex) {
                System.out.println("PitFallTrapInventoryHandlerScript: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void cleanInventory() {
        PitFallTrapHunting creature = config.pitFallTrapHunting();
        boolean keepFur = config.furHandling() == FurHandling.BANK;
        boolean keepAntlers = config.fletchAntlerBolts();

        Rs2Inventory.items().forEachOrdered(item -> {
            int itemId = item.getId();

            // Keep fur if banking, otherwise drop
            if (keepFur && itemId == creature.getFurItemId()) return;

            // Keep antlers if fletching bolts is enabled
            if (keepAntlers && itemId == creature.getAntlerItemId()) return;

            // Drop items in the creature's drop list
            if (creature.getItemsToDrop().contains(itemId)) {
                Rs2Inventory.interact(item, "Drop");
                sleep(600, 1200);
            }
        });
        script.forceDrop = false;
    }
}
