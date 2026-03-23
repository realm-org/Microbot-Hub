package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum TeasingTool {
    // TODO: Replace placeholder item IDs with actual values from game data
    TEASING_STICK("teasing stick", 10029, 0),   // ItemID for teasing stick
    HUNTERS_SPEAR("hunter's spear", 29796, 0);  // ItemID for hunter's spear — TODO: confirm attack level

    private final String itemName;
    private final int itemId;
    private final int attackLevel;

    public boolean hasRequirements() {
        if (attackLevel <= 0) return true;
        return Rs2Player.getSkillRequirement(Skill.ATTACK, attackLevel);
    }
}
