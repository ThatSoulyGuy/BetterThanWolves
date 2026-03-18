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

public class FCClientGuiCraftingAnvil extends FCClientGuiCraftingWorkbench
{
    public FCClientGuiCraftingAnvil( InventoryPlayer invPlayer, World world, int i, int j, int k )
    {
    	super( invPlayer, world, i, j, k );
    }

    @Override
    public void drawGuiContainerBackgroundLayer( float par1, int par2, int par3 )
    {
        GL11.glColor4f( 1F, 1F, 1F, 1F);
        
        mc.renderEngine.bindTexture( "/btwmodtex/fcGuiAnvilVanilla.png" );
        
        int xPos = (width - xSize) / 2;
        int yPos = (height - ySize) / 2;
        
        drawTexturedModalRect( xPos, yPos, 0, 0, xSize, ySize);
    }
}
