package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;

@ConfigInformation("<html>"
        + "Pitfall Trap Hunter by TaF"
        + "<p>This plugin automates hunting creatures with Pitfall traps.</p>\n"
        + "<p>Requirements:</p>\n"
        + "<ol>\n"
        + "    <li>Appropriate Hunter level for your chosen creature</li>\n"
        + "    <li>Teasing stick or Hunter's spear (equipped or in inventory)</li>\n"
        + "    <li>Axe in bank or inventory (for chopping logs)</li>\n"
        + "    <li>Knife in bank or inventory</li>\n"
        + "</ol>\n"
        + "<p>Cycle: Set trap on pit → Tease NPC → Jump over spiked pit → Check pit for loot</p>\n"
        + "<p>Other valuable items:</p>\n"
        + "<ol>\n"
        + "    <li>Kandarin headgear for a chance at extra logs being cut</li>\n"
        + "    <li>Graceful for banking/travel</li>\n"
        + "    <li>Guild hunter outfit for increased catch rates</li>\n"
        + "    <li>Chisel for fletching sunlight antler bolts</li>\n"
        + "</ol>\n"
        + "</html>")
@ConfigGroup("PitFallTrapHunter")
public interface PitFallTrapHunterConfig extends Config {

    // ---- Sections ----

    @ConfigSection(
            name = "General",
            description = "General hunting settings",
            position = 0
    )
    String generalSection = "General";

    @ConfigSection(
            name = "Supplies",
            description = "Axe, meat pouch, and food settings",
            position = 1
    )
    String suppliesSection = "Supplies";

    @ConfigSection(
            name = "Loot & Fletching",
            description = "How to handle antelope fur and antler bolt fletching",
            position = 2
    )
    String lootSection = "Loot & Fletching";

    @ConfigSection(
            name = "Timings",
            description = "Sleep timings after actions",
            position = 3
    )
    String timingsSection = "Timings";

    // ---- General ----

    @ConfigItem(
            section = generalSection,
            position = 0,
            keyName = "pitFallTrapHunting",
            name = "Creature to hunt",
            description = "Select which creature to hunt with pitfall traps"
    )
    default PitFallTrapHunting pitFallTrapHunting() {
        return PitFallTrapHunting.SUNLIGHT_ANTELOPE;
    }

    @ConfigItem(
            section = generalSection,
            position = 1,
            keyName = "xpMode",
            name = "XP Mode",
            description = "Disables looting and banking to maximize XP per hour"
    )
    default boolean xpMode() {
        return false;
    }

    @ConfigItem(
            section = generalSection,
            position = 2,
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Displays overlay with traps and status"
    )
    default boolean showOverlay() {
        return true;
    }

    // ---- Supplies ----

    @ConfigItem(
            section = suppliesSection,
            position = 0,
            keyName = "axeInInventory",
            name = "Use axe in inventory?",
            description = "Keep axe in inventory instead of equipping it"
    )
    default boolean axeInInventory() {
        return true;
    }

    @ConfigItem(
            section = suppliesSection,
            position = 1,
            keyName = "UseMeatPouch",
            name = "Use meat pouch?",
            description = "Do you have a meat pouch?"
    )
    default boolean UseMeatPouch() {
        return true;
    }

    @ConfigItem(
            section = suppliesSection,
            position = 2,
            keyName = "MeatPouch",
            name = "Meat pouch",
            description = "Which meat pouch should the script use?"
    )
    default MeatPouch MeatPouch() {
        return MeatPouch.LARGE_MEAT_POUCH;
    }

    @ConfigItem(
            section = suppliesSection,
            position = 3,
            keyName = "EatAtBank",
            name = "Eat at bank",
            description = "Auto eats food at the bank to fill up your hitpoints."
    )
    default boolean AutoEat() {
        return true;
    }

    @ConfigItem(
            section = suppliesSection,
            position = 4,
            keyName = "FoodToEatAtBank",
            name = "Food to eat at bank",
            description = "What food should we eat at the bank?"
    )
    default Rs2Food FoodToEatAtBank() {
        return Rs2Food.LOBSTER;
    }

    @ConfigItem(
            section = suppliesSection,
            position = 5,
            keyName = "runToBankHP",
            name = "Run to bank at HP",
            description = "Run to the bank to eat when HP drops to this level or below"
    )
    default int runToBankHP() {
        return 25;
    }

    // ---- Loot & Fletching ----

    @ConfigItem(
            section = lootSection,
            position = 0,
            keyName = "furHandling",
            name = "Antelope fur",
            description = "Drop antelope fur or bank it"
    )
    default FurHandling furHandling() {
        return FurHandling.DROP;
    }

    @ConfigItem(
            section = lootSection,
            position = 1,
            keyName = "fletchAntlerBolts",
            name = "Fletch sunlight antler bolts",
            description = "Use chisel on sunlight antlers to fletch bolts when inventory is nearly full. Requires chisel in inventory."
    )
    default boolean fletchAntlerBolts() {
        return false;
    }

    // ---- Timings ----

    @ConfigItem(
            section = timingsSection,
            position = 0,
            keyName = "MinSleepAfterCatch",
            name = "Min. Sleep After Catch (ms)",
            description = "Minimum sleep in ms after checking a caught creature"
    )
    default int minSleepAfterCatch() {
        return 4500;
    }

    @ConfigItem(
            section = timingsSection,
            position = 1,
            keyName = "MaxSleepAfterCatch",
            name = "Max. Sleep After Catch (ms)",
            description = "Maximum sleep in ms after checking a caught creature"
    )
    default int maxSleepAfterCatch() {
        return 7000;
    }

    @ConfigItem(
            section = timingsSection,
            position = 2,
            keyName = "MinSleepAfterLay",
            name = "Min. Sleep After Trap Set (ms)",
            description = "Minimum sleep in ms after setting a trap"
    )
    default int minSleepAfterLay() {
        return 2000;
    }

    @ConfigItem(
            section = timingsSection,
            position = 3,
            keyName = "MaxSleepAfterLay",
            name = "Max. Sleep After Trap Set (ms)",
            description = "Maximum sleep in ms after setting a trap"
    )
    default int maxSleepAfterLay() {
        return 5000;
    }
}
