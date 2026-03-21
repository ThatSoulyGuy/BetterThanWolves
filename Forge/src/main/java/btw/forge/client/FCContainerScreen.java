package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.FCContainerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side screen for FC container GUIs. Uses FC's original GUI textures
 * per container type (soulforge, hopper, infernal enchanter, etc.).
 */
public class FCContainerScreen extends AbstractContainerScreen<FCContainerMenu> {

    /** GUI info: texture + dimensions per FC container type. */
    private record GuiInfo(ResourceLocation texture, int width, int height) {}

    private static final Map<String, GuiInfo> GUI_MAP = new HashMap<>();
    static {
        GUI_MAP.put("FCContainerSoulforge", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguianvil.png"), 176, 184));
        GUI_MAP.put("FCContainerWorkbench", new GuiInfo(new ResourceLocation("textures/gui/container/crafting_table.png"), 176, 166));
        GUI_MAP.put("FCContainerInfernalEnchanter", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguiinfernal.png"), 176, 210));
        GUI_MAP.put("FCContainerHopper", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fchopper.png"), 176, 166));
        GUI_MAP.put("FCContainerBlockDispenser", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguiblockdisp.png"), 176, 166));
        GUI_MAP.put("FCContainerPulley", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguipulley.png"), 176, 166));
        GUI_MAP.put("FCContainerCauldron", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fccauldron.png"), 176, 193));
        GUI_MAP.put("FCContainerCrucible", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fccauldron.png"), 176, 193));
        GUI_MAP.put("FCContainerMillStone", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fccauldron.png"), 176, 193));
        GUI_MAP.put("FCContainerHamper", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguiinv4.png"), 176, 166));
        GUI_MAP.put("FCContainerFurnaceBrick", new GuiInfo(new ResourceLocation("textures/gui/container/furnace.png"), 176, 166));
        GUI_MAP.put("FCContainerVanillaAnvil", new GuiInfo(new ResourceLocation(BTWForgeMod.MOD_ID, "textures/gui/fcguianvilvanilla.png"), 176, 166));
        GUI_MAP.put("SimpleChestContainer", new GuiInfo(new ResourceLocation("textures/gui/container/generic_54.png"), 176, 222));
    }

    private static final GuiInfo FALLBACK = new GuiInfo(
            new ResourceLocation("textures/gui/container/generic_54.png"), 176, 166);

    private final ResourceLocation guiTexture;

    public FCContainerScreen(FCContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);

        String type = menu.getContainerType();
        GuiInfo info = GUI_MAP.getOrDefault(type, FALLBACK);
        this.guiTexture = info.texture;
        this.imageWidth = info.width;
        this.imageHeight = info.height;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(this.guiTexture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }
}
