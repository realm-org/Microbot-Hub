package net.runelite.client.plugins.microbot.aiofighter.cannon;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldArea;
import net.runelite.client.plugins.cannon.CannonPlugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.aiofighter.enums.State;
import net.runelite.client.plugins.microbot.util.Global;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2Cannon;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

public class CannonScript extends Script {

    private static final int CANNON_REFILL_AMOUNT = 5;

    public boolean run(AIOFighterConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run() || !config.toggleCannon()) return;

                if (config.state().equals(State.BANKING) || config.state().equals(State.WALKING))
                    return;

                if (Rs2Cannon.repair())
                    return;

                refill(CANNON_REFILL_AMOUNT);
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean refill(int cannonRefillAmount) {
        if (!Rs2Inventory.hasItemAmount("cannonball", 15, true)) {
            return false;
        }

        int cannonBallsLeft = Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getVarpValue(3)).orElse(0);

        if (cannonBallsLeft > cannonRefillAmount) {
            return false;
        }

        Microbot.status = "Refilling Cannon";
        TileObject cannon = Rs2GameObject.findObject(new Integer[]{6, 43027});
        if (cannon == null) {
            return false;
        }

        WorldArea cannonLocation = new WorldArea(
                cannon.getWorldLocation().getX() - 1,
                cannon.getWorldLocation().getY() - 1,
                3, 3,
                cannon.getWorldLocation().getPlane());

        if (!cannonLocation.toWorldPoint().equals(CannonPlugin.getCannonPosition().toWorldPoint())) {
            return false;
        }

        Microbot.pauseAllScripts.compareAndSet(false, true);
        Rs2Inventory.useItemOnObject(2, cannon.getId());
        Rs2Player.waitForWalking(5000);
        Global.sleepUntil(() -> Microbot.getClientThread().runOnClientThreadOptional(
                () -> Microbot.getClient().getVarpValue(3)).orElse(0) > Rs2Random.between(10, 15));
        Microbot.pauseAllScripts.compareAndSet(true, false);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
