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

//FCMOD


import org.lwjgl.opengl.GL11;

public class FCClientGuiHamper extends GuiContainer
{
	private static final int m_iHamperGuiHeight = 149;

    private IInventory m_hamperInventory;

	public FCClientGuiHamper( InventoryPlayer playerInventory, IInventory hamperInventory )
    {
        super( new FCContainerHamper( playerInventory, hamperInventory ) );
        
        ySize = m_iHamperGuiHeight;
        
        m_hamperInventory = hamperInventory;
    }

    @Override
    public void drawGuiContainerForegroundLayer( int i, int j )
    {
        String windowName = StatCollector.translateToLocal( m_hamperInventory.getInvName() );
        
        fontRenderer.drawString( windowName, xSize / 2 - fontRenderer.getStringWidth( windowName ) / 2, 6, 0x404040 );
        
        fontRenderer.drawString( StatCollector.translateToLocal( "container.inventory" ), 8, ( ySize - 96 ) + 2, 0x404040 );
    }

    @Override
    public void drawGuiContainerBackgroundLayer( float f, int i, int j )
    {
    	// draw the background image
    	
        GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
        
        mc.renderEngine.bindTexture( "/btwmodtex/fcGuiInv4.png" );
        
        int xPos = ( width - xSize ) / 2;
        int yPos = ( height - ySize ) / 2;
        
        drawTexturedModalRect( xPos, yPos, 0, 0, xSize, ySize );        
    }    
}