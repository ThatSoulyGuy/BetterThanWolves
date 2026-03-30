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
 *
 * <p>For the Infernal Enchanter, renders clickable enchantment buttons
 * whose levels are synced from the server via FC DataSlots.</p>
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

    // Enchanter button constants (from FCClientGuiInfernalEnchanter)
    private static final int ENCHANT_BUTTONS_X = 60;
    private static final int ENCHANT_BUTTONS_Y = 17;
    private static final int ENCHANT_BUTTON_W = 108;
    private static final int ENCHANT_BUTTON_H = 19;
    private static final int ENCHANT_BUTTON_COUNT = 5;

    private final ResourceLocation guiTexture;
    private final boolean isEnchanter;
    private final int chestRows;

    public FCContainerScreen(FCContainerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);

        String type = menu.getContainerType();
        GuiInfo info = GUI_MAP.getOrDefault(type, FALLBACK);

        // For chest containers, calculate height based on actual row count
        int rows = 0;
        if ("SimpleChestContainer".equals(type)) {
            rows = menu.getFcNumRows();
            int height = 114 + rows * 18;
            info = new GuiInfo(new ResourceLocation("textures/gui/container/generic_54.png"), 176, height);
        }

        this.chestRows = rows;
        this.guiTexture = info.texture;
        this.imageWidth = info.width;
        this.imageHeight = info.height;
        this.inventoryLabelY = this.imageHeight - 94;
        this.isEnchanter = "FCContainerInfernalEnchanter".equals(type);
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

        if (chestRows > 0 && chestRows < 6) {
            // Chest GUI: blit in two parts from generic_54.png
            // Top: header (17px) + chest rows
            int topHeight = 17 + chestRows * 18;
            graphics.blit(this.guiTexture, x, y, 0, 0, this.imageWidth, topHeight);
            // Bottom: player inventory (96px) from the bottom of the texture
            int playerInvTexY = 126; // where player inventory starts in generic_54.png
            graphics.blit(this.guiTexture, x, y + topHeight, 0, playerInvTexY, this.imageWidth, 96);
        } else {
            graphics.blit(this.guiTexture, x, y, 0, 0, this.imageWidth, this.imageHeight);
        }

        if (isEnchanter) {
            renderEnchanterButtons(graphics, x, y, mouseX, mouseY);
        }
    }

    /**
     * Renders the 5 enchantment buttons for the Infernal Enchanter.
     * Button state comes from FC DataSlots synced by the server:
     * <ul>
     *   <li>fcData[0]: bookshelf level</li>
     *   <li>fcData[1..5]: enchantment level costs</li>
     * </ul>
     */
    private void renderEnchanterButtons(GuiGraphics graphics, int guiX, int guiY,
                                         int mouseX, int mouseY) {
        int bookshelfLevel = this.menu.getFcData(0);
        int playerLevel = this.minecraft.player != null
                ? this.minecraft.player.experienceLevel : 0;

        // Reconstruct the enchantment name from the scroll in slot 0
        String enchantName = null;
        if (this.menu.slots.size() > 0) {
            net.minecraft.world.item.ItemStack scrollStack = this.menu.slots.get(0).getItem();
            if (!scrollStack.isEmpty()) {
                int scrollDamage = scrollStack.getDamageValue();
                // Look up the enchantment name via FC's translation
                if (scrollDamage >= 0 && scrollDamage < btw.modern.Enchantment.enchantmentsList.length) {
                    btw.modern.Enchantment ench = btw.modern.Enchantment.enchantmentsList[scrollDamage];
                    if (ench != null) {
                        enchantName = btw.modern.StatCollector.translateToLocal(ench.getName());
                    }
                }
            }
        }

        for (int i = 0; i < ENCHANT_BUTTON_COUNT; i++) {
            int enchantLevel = this.menu.getFcData(i + 1);
            int btnX = guiX + ENCHANT_BUTTONS_X;
            int btnY = guiY + ENCHANT_BUTTONS_Y + i * ENCHANT_BUTTON_H;

            if (enchantLevel <= 0) {
                // Draw inactive button overlay to cover the background button area
                graphics.blit(this.guiTexture, btnX, btnY, 0, 230,
                        ENCHANT_BUTTON_W, ENCHANT_BUTTON_H);
                continue;
            }

            boolean canAfford = enchantLevel <= playerLevel
                    && enchantLevel <= bookshelfLevel;
            boolean hovered = mouseX >= btnX && mouseX < btnX + ENCHANT_BUTTON_W
                    && mouseY >= btnY && mouseY < btnY + ENCHANT_BUTTON_H;

            int texU;
            int texV;
            if (!canAfford) {
                texU = 0;
                texV = 230;
            } else if (hovered) {
                texU = 108;
                texV = 211;
            } else {
                texU = 0;
                texV = 211;
            }

            graphics.blit(this.guiTexture, btnX, btnY, texU, texV,
                    ENCHANT_BUTTON_W, ENCHANT_BUTTON_H);

            // Render enchantment name + level (e.g., "Fire Aspect II")
            String tierName = enchantName != null
                    ? enchantName + " " + toRoman(i + 1)
                    : "Level " + (i + 1);
            int nameColor = canAfford ? 0x685e4a : 0x403830;
            if (hovered && canAfford) nameColor = 0xffff80;
            String trimmed = this.font.plainSubstrByWidth(tierName, ENCHANT_BUTTON_W - 14);
            graphics.drawString(this.font, trimmed, btnX + 2, btnY + 2, nameColor, false);

            // Render enchantment level cost (right-aligned)
            String levelStr = String.valueOf(enchantLevel);
            int textColor = canAfford ? 0x80ff20 : 0x407f10;
            int textX = btnX + ENCHANT_BUTTON_W - 2 - this.font.width(levelStr);
            int textY = btnY + 9;
            graphics.drawString(this.font, levelStr, textX, textY, textColor, true);
        }
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY,
                4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isEnchanter && button == 0) {
            int guiX = (this.width - this.imageWidth) / 2;
            int guiY = (this.height - this.imageHeight) / 2;

            for (int i = 0; i < ENCHANT_BUTTON_COUNT; i++) {
                int enchantLevel = this.menu.getFcData(i + 1);
                if (enchantLevel <= 0) continue;

                int btnX = guiX + ENCHANT_BUTTONS_X;
                int btnY = guiY + ENCHANT_BUTTONS_Y + i * ENCHANT_BUTTON_H;

                if (mouseX >= btnX && mouseX < btnX + ENCHANT_BUTTON_W
                        && mouseY >= btnY && mouseY < btnY + ENCHANT_BUTTON_H) {
                    // Send enchant button click to server
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(
                                this.menu.containerId, i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
