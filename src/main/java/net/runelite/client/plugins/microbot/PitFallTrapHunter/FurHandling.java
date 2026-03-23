package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FurHandling {
    DROP("Drop"),
    BANK("Bank");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
