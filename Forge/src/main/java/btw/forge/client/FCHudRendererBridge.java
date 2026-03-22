package btw.forge.client;

import btw.forge.BTWNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * Bridge that renders FC's custom HUD elements using modern MC's GuiGraphics.
 *
 * <p>This wraps FC's original DrawFoodOverlay / DrawPenaltyText / DrawSightlessText
 * logic (from FC's patched GuiIngame, patch.txt lines 20232-20405) and renders
 * it through Forge's overlay system. All FC state is read from
 * {@link BTWNetwork} (synced from server) — no FC code is modified.</p>
 *
 * <p>The food pip rendering uses vanilla's {@code icons.png} texture atlas
 * with the same UV coordinates FC used in 1.5.2, since FC's food pips
 * reuse vanilla's shank icon slots.</p>
 */
public class FCHudRendererBridge {

    private static final ResourceLocation ICONS =
            new ResourceLocation("minecraft", "textures/gui/icons.png");

    private final Random rand = new Random();
    private int updateCounter = 0;
    private int foodOverlayShakeCounter = 0;

    // Track previous food level to detect exhaustion (food decrease = shake trigger)
    private int prevFoodLevel = 60;

    public void tick() {
        updateCounter++;
    }

    /**
     * Renders the FC food overlay: 10 pips using FC's 0-60 food scale.
     * Verbatim port of FC's DrawFoodOverlay from patched GuiIngame.
     */
    public void renderFoodOverlay(GuiGraphics gui, int screenX, int screenY) {
        Minecraft mc = Minecraft.getInstance();

        int foodLevel = BTWNetwork.clientFoodLevel;
        float saturation = BTWNetwork.clientSaturation;
        int hungerPenalty = BTWNetwork.clientHungerPenalty;
        boolean hasHungerPotion = mc.player != null &&
                mc.player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);

        int saturationPips = (int) ((saturation + 0.124F) * 4F);
        int fullHungerPips = foodLevel / 6;

        // Detect exhaustion: food level decreased since last frame
        if (foodLevel < prevFoodLevel) {
            foodOverlayShakeCounter = 20;
        }
        prevFoodLevel = foodLevel;

        if (foodOverlayShakeCounter > 0) {
            foodOverlayShakeCounter--;
        }

        for (int i = 0; i < 10; ++i) {
            int shankY = screenY;
            int shankTexOffsetX = 16;
            int bgTexOffsetX = 0;

            if (hasHungerPotion) {
                shankTexOffsetX += 36;
                bgTexOffsetX = 13;
            } else if (i < saturationPips >> 3) {
                bgTexOffsetX = 1;
            }

            // Shake when hungry
            if (hungerPenalty > 0 && updateCounter % (foodLevel * 5 + 1) == 0) {
                shankY = screenY + (rand.nextInt(3) - 1);
            } else if (foodOverlayShakeCounter > 0) {
                int shake = 1;
                if (rand.nextInt(2) == 0) shake = -shake;
                shankY = screenY + shake;
            }

            int shankX = screenX - i * 8 - 9;

            // Background pip
            gui.blit(ICONS, shankX, shankY, 16 + bgTexOffsetX * 9, 27, 9, 9);

            // Partial saturation pip
            if (i == saturationPips >> 3 && !hasHungerPotion) {
                int partial = saturationPips % 8;
                if (partial != 0) {
                    gui.blit(ICONS, shankX + 8 - partial, shankY,
                            25 + 8 - partial, 27, 1 + partial, 9);
                }
            }

            // Full hunger pip
            if (i < fullHungerPips) {
                gui.blit(ICONS, shankX, shankY, shankTexOffsetX + 36, 27, 9, 9);
            }
            // Partial hunger pip
            else if (i == fullHungerPips) {
                int partial = foodLevel % 6;
                if (partial != 0) {
                    gui.blit(ICONS, shankX + 7 - partial, shankY,
                            shankTexOffsetX + 36 + 7 - partial, 27,
                            3 + partial, 9);
                }
            }
        }
    }

    /**
     * Draws the highest-priority penalty text. Port of FC's DrawPenaltyText.
     * @return true if a penalty string was drawn
     */
    public boolean renderPenaltyText(GuiGraphics gui, int screenX, int screenY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) return false;

        int healthP = BTWNetwork.clientHealthPenalty;
        int hungerP = BTWNetwork.clientHungerPenalty;
        int fatP = BTWNetwork.clientFatPenalty;

        String text = null;
        if (healthP > 0 && healthP >= hungerP && healthP >= fatP) {
            text = HEALTH_PENALTY[Math.min(healthP, HEALTH_PENALTY.length - 1)];
        } else if (hungerP > 0 && hungerP >= fatP) {
            text = HUNGER_PENALTY[Math.min(hungerP, HUNGER_PENALTY.length - 1)];
        } else if (fatP > 0) {
            text = FAT_PENALTY[Math.min(fatP, FAT_PENALTY.length - 1)];
        }

        if (text != null) {
            int w = mc.font.width(text);
            gui.drawString(mc.font, text, screenX - w, screenY, 0xFFFFFF, true);
            return true;
        }
        return false;
    }

    /**
     * Draws gloom level text. Port of FC's DrawSightlessText.
     */
    public void renderGloomText(GuiGraphics gui, int screenX, int screenY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) return;

        int gloom = BTWNetwork.clientGloomLevel;
        if (gloom <= 0) return;

        String text;
        switch (gloom) {
            case 2: text = "Dread"; break;
            case 3: text = "Terror"; break;
            default: text = "Gloom"; break;
        }

        int w = mc.font.width(text);
        gui.drawString(mc.font, text, screenX - w, screenY, 0xFFFFFF, true);
    }

    private static final String[] HEALTH_PENALTY = {
            "Invalid", "Hurt", "Injured", "Wounded", "Crippled", "Dying"
    };
    private static final String[] HUNGER_PENALTY = {
            "Invalid", "Peckish", "Hungry", "Famished", "Starving", "Dying"
    };
    private static final String[] FAT_PENALTY = {
            "Invalid", "Plump", "Chubby", "Fat", "Obese", "Invalid"
    };
}
