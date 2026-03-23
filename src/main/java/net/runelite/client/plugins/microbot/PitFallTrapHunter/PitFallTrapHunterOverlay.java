package net.runelite.client.plugins.microbot.PitFallTrapHunter;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class PitFallTrapHunterOverlay extends OverlayPanel {
    private final Client client;
    private final PitFallTrapHunterConfig config;
    private final PitFallTrapHunterPlugin plugin;
    private final PitFallTrapHunterScript script;
    private int startingLevel = 0;

    @Inject
    public PitFallTrapHunterOverlay(Client client, PitFallTrapHunterConfig config, PitFallTrapHunterPlugin plugin, PitFallTrapHunterScript script) {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        if (startingLevel == 0) {
            startingLevel = client.getRealSkillLevel(Skill.HUNTER);
        }

        panelComponent.getChildren().clear();
        panelComponent.setPreferredSize(new Dimension(200, 300));

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Pitfall Trap Hunter by TaF")
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder().build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Running: ")
                .right(plugin.getTimeRunning())
                .leftColor(Color.WHITE)
                .rightColor(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Hunter Level:")
                .right(startingLevel + "/" + client.getRealSkillLevel(Skill.HUNTER))
                .leftColor(Color.WHITE)
                .rightColor(Color.ORANGE)
                .build());

        if (config.pitFallTrapHunting() != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hunting:")
                    .right(config.pitFallTrapHunting().getName())
                    .leftColor(Color.WHITE)
                    .rightColor(Color.YELLOW)
                    .build());
        }

        // Current state
        panelComponent.getChildren().add(LineComponent.builder()
                .left("State:")
                .right(script.getState().name())
                .leftColor(Color.WHITE)
                .rightColor(Color.CYAN)
                .build());

        // Traps information
        int currentTraps = plugin.getTraps().size();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Traps:")
                .right(String.valueOf(currentTraps))
                .leftColor(Color.WHITE)
                .rightColor(currentTraps > 0 ? Color.GREEN : Color.CYAN)
                .build());

        // Statistics
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Creatures Caught:")
                .right(String.valueOf(PitFallTrapHunterScript.creaturesCaught))
                .leftColor(Color.WHITE)
                .rightColor(Color.GREEN)
                .build());

        // Fletching status
        if (config.fletchAntlerBolts()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Bolt Fletching:")
                    .right("Enabled")
                    .leftColor(Color.WHITE)
                    .rightColor(Color.MAGENTA)
                    .build());
        }

        // Fur handling
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Fur:")
                .right(config.furHandling().getName())
                .leftColor(Color.WHITE)
                .rightColor(config.furHandling() == FurHandling.BANK ? Color.YELLOW : Color.GRAY)
                .build());

        return super.render(graphics);
    }
}
