package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.hunter.HunterTrap;
import net.runelite.client.plugins.microbot.PluginConstants;
import net.runelite.client.plugins.microbot.util.misc.TimeUtils;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@PluginDescriptor(
        name = PluginDescriptor.TaFCat + "Pitfall Trap Hunter",
        description = "Automates hunting creatures with Pitfall traps (Sunlight Antelope).",
        tags = {"hunter", "pitfall", "antelope", "skilling", "xp", "loot", "TaF"},
        version = PitFallTrapHunterPlugin.version,
        minClientVersion = "2.1.0",
        cardUrl = "",
        iconUrl = "",
        enabledByDefault = PluginConstants.DEFAULT_ENABLED,
        isExternal = PluginConstants.IS_EXTERNAL
)
public class PitFallTrapHunterPlugin extends Plugin {

    public static final String version = "1.0.0";

    // TODO: Replace these placeholder object IDs with actual values from in-game dev tools
    // These are used in onGameObjectSpawned to track trap states
    private static final int PIT_UNSET_ID = -1;          // Unset pit (collapsed/empty)
    private static final int PIT_SET_ID = -1;             // Pit after "Trap" interaction (set, waiting)
    private static final int PIT_SPIKED_ID = -1;          // Spiked pit ready for "Jump"
    private static final int PIT_FULL_ANTELOPE_ID = -1;   // Pit with caught Sunlight Antelope
    private static final int PIT_TRAPPING_ID = -1;        // Transition state while creature falls in

    @Getter
    private final Map<WorldPoint, HunterTrap> traps = new HashMap<>();
    @Inject
    private Client client;
    @Inject
    private PitFallTrapHunterConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PitFallTrapHunterOverlay overlay;
    private PitFallTrapHunterScript script;
    private PitFallTrapInventoryHandlerScript inventoryHandler;
    private Instant scriptStartTime;

    @Provides
    PitFallTrapHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PitFallTrapHunterConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Pitfall Trap Hunter plugin started!");
        scriptStartTime = Instant.now();
        overlayManager.add(overlay);
        script = new PitFallTrapHunterScript();
        script.run(config, this);
        inventoryHandler = new PitFallTrapInventoryHandlerScript();
        inventoryHandler.run(config, script);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Pitfall Trap Hunter plugin stopped!");
        scriptStartTime = null;
        overlayManager.remove(overlay);
        if (script != null) {
            script.shutdown();
            inventoryHandler.shutdown();
        }
    }

    protected String getTimeRunning() {
        return scriptStartTime != null ? TimeUtils.getFormattedDurationBetween(scriptStartTime, Instant.now()) : "";
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();
        final WorldPoint trapLocation = gameObject.getWorldLocation();
        final HunterTrap myTrap = traps.get(trapLocation);
        final Player localPlayer = client.getLocalPlayer();
        final int objectId = gameObject.getId();

        // Using if-else instead of switch because placeholder IDs are all -1 during development.
        // Replace with a proper switch once real object IDs are filled in.
        if (objectId == PIT_SET_ID || objectId == PIT_SPIKED_ID) {
            // Trap placed — player interacted with "Trap" on pit
            if (localPlayer.getWorldLocation().distanceTo(trapLocation) <= 3) {
                log.debug("Pitfall trap set by \"{}\" at {}", localPlayer.getName(), trapLocation);
                traps.put(trapLocation, new HunterTrap(gameObject));
            }
        } else if (objectId == PIT_FULL_ANTELOPE_ID) {
            // Creature caught — pit is full
            if (myTrap != null) {
                myTrap.setState(HunterTrap.State.FULL);
                myTrap.resetTimer();
            }
        } else if (objectId == PIT_UNSET_ID) {
            // Pit collapsed/reset — empty again
            if (myTrap != null) {
                myTrap.setState(HunterTrap.State.EMPTY);
                myTrap.resetTimer();
            }
        } else if (objectId == PIT_TRAPPING_ID) {
            // Transition — creature falling in
            if (myTrap != null) {
                myTrap.setState(HunterTrap.State.TRANSITION);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.GAMEMESSAGE) {
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("oh dear, you are dead!")) {
                script.hasDied = true;
            }
            if (msg.contains("You don't have enough inventory space. You need")) {
                script.forceBank = true;
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Iterator<Map.Entry<WorldPoint, HunterTrap>> it = traps.entrySet().iterator();
        Tile[][][] tiles = client.getScene().getTiles();
        Instant expire = Instant.now().minus(HunterTrap.TRAP_TIME.multipliedBy(2));

        while (it.hasNext()) {
            Map.Entry<WorldPoint, HunterTrap> entry = it.next();
            HunterTrap trap = entry.getValue();
            WorldPoint world = entry.getKey();
            LocalPoint local = LocalPoint.fromWorld(client, world);

            if (local == null) {
                if (trap.getPlacedOn().isBefore(expire)) {
                    log.debug("Pitfall trap removed due to timeout, {} left", traps.size());
                    it.remove();
                }
                continue;
            }

            Tile tile = tiles[world.getPlane()][local.getSceneX()][local.getSceneY()];
            GameObject[] objects = tile.getGameObjects();

            boolean containsAnything = false;
            boolean containsUnsetPit = false;
            for (GameObject object : objects) {
                if (object != null) {
                    containsAnything = true;
                    if (object.getId() == PIT_UNSET_ID) {
                        containsUnsetPit = true;
                        break;
                    }
                }
            }

            if (!containsAnything) {
                it.remove();
                log.debug("Pitfall trap removed (no objects), {} left", traps.size());
            } else if (containsUnsetPit) {
                it.remove();
                log.debug("Pitfall trap removed (pit collapsed), {} left", traps.size());
            }
        }
    }
}
