package net.minecraft.src.btw.tileentity;

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

public class FCTileEntityCampfireRenderer extends TileEntitySpecialRenderer
{
    public void renderTileEntityAt( TileEntity tileEntity, double xCoord, double yCoord, double zCoord, float fPartialTickCount )
    {
    	FCTileEntityCampfire campfire = (FCTileEntityCampfire)tileEntity;
    	
		RenderCookStack( campfire, xCoord, yCoord, zCoord );
    }
    
    private void RenderCookStack( FCTileEntityCampfire campfire, double xCoord, double yCoord, double zCoord )
    {
    	ItemStack stack = campfire.GetCookStack();
    	
    	if ( stack != null )
    	{
	    	int iMetadata = campfire.worldObj.getBlockMetadata( campfire.xCoord, campfire.yCoord, campfire.zCoord );
	    	boolean bIAligned = FCBetterThanWolves.fcBlockCampfireUnlit.GetIsIAligned( iMetadata );
	    	
	        EntityItem entity = new EntityItem( campfire.worldObj, 0.0D, 0.0D, 0.0D, stack );
	        
	        entity.getEntityItem().stackSize = 1;
	        entity.hoverStart = 0.0F;
	    	
	        GL11.glPushMatrix();
	        GL11.glTranslatef( (float)xCoord + 0.5F, (float)yCoord + ( 9F / 16F ), (float)zCoord + 0.5F );        
	        
	        if ( !bIAligned && RenderManager.instance.options.fancyGraphics )
	        {        	
	            // don't rotate items rendered as billboards (fancyGraphics test)
	            
	        	GL11.glRotatef( 90F, 0.0F, 1.0F, 0.0F);
	        }
	
	        RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
	
	        GL11.glPopMatrix();
    	}
    }
}
