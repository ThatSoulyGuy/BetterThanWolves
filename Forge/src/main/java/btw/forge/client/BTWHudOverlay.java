package btw.forge.client;

import btw.forge.BTWNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side HUD overlay that renders FC penalty text and the FC hunger bar.
 *
 * <p>FC replaces vanilla's hunger bar with a 60-unit food system (10 pips,
 * each worth 6 food units) and displays penalty text strings above the
 * hotbar when the player is suffering from health, hunger, fat, or gloom
 * penalties.</p>
 *
 * <p>The penalty levels and food level are received from the server via
 * {@link BTWNetwork.PenaltySync} packets and stored in
 * {@link BTWNetwork#clientHungerPenalty} etc.</p>
 *
 * <p>This is registered on the MOD event bus via {@code @Mod.EventBusSubscriber}
 * so that {@link #registerOverlays(RegisterGuiOverlaysEvent)} fires during
 * mod loading.</p>
 *
 * <h3>FC original rendering (GuiIngame.java lines 1388-1555):</h3>
 * <ul>
 *   <li>{@code DrawFoodOverlay}: 10 food pips with full/partial/empty states,
 *       shaking animation, saturation background, hunger potion tinting</li>
 *   <li>{@code DrawPenaltyText}: shows highest of health/hunger/fat penalty
 *       as a single word (e.g. "Famished", "Crippled", "Obese")</li>
 *   <li>{@code DrawSightlessText}: shows gloom level as "Gloom"/"Dread"/"Terror"</li>
 * </ul>
 *
 * <p>For now this implements the text-based penalty display. The full pip-based
 * food bar rendering (with texture atlas references) will be added later when
 * the FC GUI texture atlas is ported.</p>
 */
@Mod.EventBusSubscriber(
        modid = btw.forge.BTWForgeMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public class BTWHudOverlay {

    // =====================================================================
    // Penalty description strings — matching FC's GuiIngame arrays
    // =====================================================================

    /** Health penalty descriptions indexed by penalty level (1-5). Index 0 is unused. */
    private static final String[] HEALTH_PENALTY = {
            "Invalid", "Hurt", "Injured", "Wounded", "Crippled", "Dying"
    };

    /** Hunger penalty descriptions indexed by penalty level (1-5). Index 0 is unused. */
    private static final String[] HUNGER_PENALTY = {
            "Invalid", "Peckish", "Hungry", "Famished", "Starving", "Dying"
    };

    /** Fat penalty descriptions indexed by penalty level (1-4). Index 0 is unused. */
    private static final String[] FAT_PENALTY = {
            "Invalid", "Plump", "Chubby", "Fat", "Obese", "Invalid"
    };

    /** Gloom level descriptions indexed by gloom level (1-3). */
    private static final String[] GLOOM_TEXT = {
            "", "Gloom", "Dread", "Terror"
    };

    // =====================================================================
    // Overlay registration
    // =====================================================================

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("btw_penalties", BTWHudOverlay::renderPenaltyOverlay);
    }

    // =====================================================================
    // Rendering
    // =====================================================================

    /**
     * Main overlay render callback. Draws penalty text and gloom text
     * in the positions matching FC's original HUD layout.
     *
     * <p>FC's penalty text is drawn to the left of the food bar area.
     * FC's gloom text is drawn below the penalty text (or in the penalty
     * text position if no penalty is active).</p>
     *
     * @param forgeGui    the Forge GUI instance
     * @param guiGraphics the graphics context for rendering
     * @param partialTick partial tick for interpolation
     * @param width       screen width
     * @param height      screen height
     */
    private static void renderPenaltyOverlay(
            net.minecraftforge.client.gui.overlay.ForgeGui forgeGui,
            GuiGraphics guiGraphics,
            float partialTick,
            int width,
            int height) {

        Minecraft mc = Minecraft.getInstance();

        // Don't render when the player is dead, in a screen, or the HUD is hidden
        if (mc.player == null || mc.player.isDeadOrDying()) return;
        if (mc.options.hideGui) return;

        // FC positions the penalty text relative to the right side of the screen,
        // just above the hotbar, aligned with the food bar area.
        // The food bar is at: x = width/2 + 91, y = height - 39 (right side of hotbar)
        // Penalty text is drawn right-aligned at the same X, one line above the food bar.
        int foodBarX = width / 2 + 91;
        int foodBarY = height - 39;

        // -- Draw penalty text (highest priority penalty) --
        boolean drewPenalty = drawPenaltyText(guiGraphics, mc, foodBarX, foodBarY - 10);

        // -- Draw gloom text (below penalty, or in penalty position if no penalty) --
        int gloomY = drewPenalty ? foodBarY : foodBarY - 10;
        drawGloomText(guiGraphics, mc, foodBarX, gloomY);
    }

    /**
     * Draws the highest-priority penalty text string.
     *
     * <p>FC prioritises penalties in this order: health >= hunger >= fat.
     * The highest penalty level wins; ties go to the earlier category.</p>
     *
     * @return true if a penalty string was drawn
     */
    private static boolean drawPenaltyText(GuiGraphics guiGraphics, Minecraft mc,
                                           int screenX, int screenY) {
        int healthPenalty = BTWNetwork.clientHealthPenalty;
        int hungerPenalty = BTWNetwork.clientHungerPenalty;
        int fatPenalty = BTWNetwork.clientFatPenalty;

        String penaltyString = null;

        if (healthPenalty > 0 && healthPenalty >= hungerPenalty && healthPenalty >= fatPenalty) {
            if (healthPenalty < HEALTH_PENALTY.length) {
                penaltyString = HEALTH_PENALTY[healthPenalty];
            }
        } else if (hungerPenalty > 0 && hungerPenalty >= fatPenalty) {
            if (hungerPenalty < HUNGER_PENALTY.length) {
                penaltyString = HUNGER_PENALTY[hungerPenalty];
            }
        } else if (fatPenalty > 0) {
            if (fatPenalty < FAT_PENALTY.length) {
                penaltyString = FAT_PENALTY[fatPenalty];
            }
        }

        if (penaltyString != null) {
            int stringWidth = mc.font.width(penaltyString);
            guiGraphics.drawString(mc.font, penaltyString,
                    screenX - stringWidth, screenY, 0xFFFFFF, true);
            return true;
        }

        return false;
    }

    /**
     * Draws the gloom level text string ("Gloom", "Dread", or "Terror").
     *
     * <p>Only drawn when gloom level > 0. The text is right-aligned at
     * the given screen position, matching FC's original layout.</p>
     */
    private static void drawGloomText(GuiGraphics guiGraphics, Minecraft mc,
                                      int screenX, int screenY) {
        int gloomLevel = BTWNetwork.clientGloomLevel;

        if (gloomLevel > 0 && gloomLevel < GLOOM_TEXT.length) {
            String gloomString = GLOOM_TEXT[gloomLevel];
            int stringWidth = mc.font.width(gloomString);
            guiGraphics.drawString(mc.font, gloomString,
                    screenX - stringWidth, screenY, 0xFFFFFF, true);
        }
    }
}
