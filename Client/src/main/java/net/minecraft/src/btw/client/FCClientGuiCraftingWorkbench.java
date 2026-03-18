package net.minecraft.src.btw.client;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD (client only)


import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class FCClientGuiCraftingWorkbench extends GuiContainer
{
	private FCContainerWorkbench m_container;
	
    public FCClientGuiCraftingWorkbench( InventoryPlayer inventory, World world, int i, int j, int k )
    {
        super( new FCContainerWorkbench( inventory, world, i, j, k ) );
        
        m_container = (FCContainerWorkbench)inventorySlots;
    }

    @Override
    public void drawGuiContainerForegroundLayer(int par1, int par2)
    {
        this.fontRenderer.drawString(StatCollector.translateToLocal("container.crafting"), 28, 6, 4210752);
        this.fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, this.ySize - 96 + 2, 4210752);

        DrawSecondaryOutputIndicator();        
    }

    @Override
    public void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.renderEngine.bindTexture("/gui/crafting.png");
        int var4 = (this.width - this.xSize) / 2;
        int var5 = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(var4, var5, 0, 0, this.xSize, this.ySize);
    }
    
    //------------- Class Specific Methods ------------//
    
    private void DrawSecondaryOutputIndicator()
    {
        IRecipe recipe = CraftingManager.getInstance().FindMatchingRecipe(
        	m_container.craftMatrix, mc.theWorld );
        
        if ( recipe != null && recipe.HasSecondaryOutput() )
        {
	        Slot outputSlot = (Slot)m_container.inventorySlots.get( 0 );
	        
	        int iDisplayX = outputSlot.xDisplayPosition + 24;
	        int iDisplayY = outputSlot.yDisplayPosition + 5;
	        
	        FCClientUtilsRender.DrawSecondaryCraftingOutputIndicator( mc, iDisplayX, iDisplayY );
        }
    }    
}
