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

// FCMOD


import org.lwjgl.opengl.GL11;

// copied wholesale from GuiDispenser with only slight modifications
public class FCClientGuiBlockDispenser extends GuiContainer
{
	static final int iSelectionIconHeight = 20;
	static final int iGuiHeight = 182;

    private FCTileEntityBlockDispenser associatedTileEntityBlockDispenser;

    public FCClientGuiBlockDispenser( InventoryPlayer inventoryplayer, FCTileEntityBlockDispenser tileentitydispenser )
    {
        super(new FCContainerBlockDispenser(inventoryplayer, tileentitydispenser));
        
        associatedTileEntityBlockDispenser = tileentitydispenser;
        
        ySize = iGuiHeight;
    }

    @Override
    public void drawGuiContainerForegroundLayer( int i, int j )
    {
        fontRenderer.drawString("Block Dispenser", 48, 6, 0x404040);
        fontRenderer.drawString("Inventory", 8, (ySize - 94) + 2, 0x404040);
    }

    @Override
    public void drawGuiContainerBackgroundLayer( float f, int i, int j )
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture( "/btwmodtex/fcguiblockdisp.png" );
        
        int xPos = (width - xSize) / 2;
        int yPos = (height - ySize) / 2;
        drawTexturedModalRect( xPos, yPos, 0, 0, xSize, ySize);
        
        // draw the selection rectangle
        
        int iXOffset = ( associatedTileEntityBlockDispenser.iNextSlotIndexToDispense % 4 ) * 18;
        int iYOffset = ( associatedTileEntityBlockDispenser.iNextSlotIndexToDispense / 4 ) * 18;
        
        drawTexturedModalRect( xPos + 51 + iXOffset,					// screen x pos 
    		yPos + 15 + iYOffset, 										// screen y pos
    		176, 														// bitmap source x
    		0, 															// bitmap source y
    		iSelectionIconHeight, 										// width
    		iSelectionIconHeight );										// height
    }
}