package btw.forge.client;

import btw.forge.BTWForgeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge overlay that replaces vanilla's food bar with FC's HUD.
 * Delegates all rendering to {@link FCHudRendererBridge}.
 */
@Mod.EventBusSubscriber(
        modid = BTWForgeMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public class BTWHudOverlay {

    private static final FCHudRendererBridge HUD = new FCHudRendererBridge();

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("btw_food", BTWHudOverlay::renderOverlay);
    }

    /** Cancels vanilla food bar. Registered on FORGE bus. */
    @Mod.EventBusSubscriber(
            modid = BTWForgeMod.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = net.minecraftforge.api.distmarker.Dist.CLIENT
    )
    public static class VanillaFoodCanceller {
        @SubscribeEvent
        public static void onPreRenderFood(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
                event.setCanceled(true);
            }
        }
    }

    private static void renderOverlay(
            net.minecraftforge.client.gui.overlay.ForgeGui forgeGui,
            GuiGraphics guiGraphics,
            float partialTick,
            int width,
            int height) {

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) return;
        if (mc.options.hideGui) return;
        if (mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;

        HUD.tick();

        // FC positions: right side of hotbar
        int foodBarX = width / 2 + 91;
        int foodBarY = height - 39;

        // FC food pips
        HUD.renderFoodOverlay(guiGraphics, foodBarX, foodBarY);

        // Penalty text + gloom (matching FC's original layout from GuiIngame)
        int sightlessTextOffset = -8;
        if (!HUD.renderPenaltyText(guiGraphics, foodBarX, foodBarY - 10)) {
            sightlessTextOffset = 0;
        }
        HUD.renderGloomText(guiGraphics, foodBarX, foodBarY - 10 + sightlessTextOffset);
    }
}
