package net.minecraft.src.btw.block;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


public class FCBlockNote extends BlockNote
{
    public FCBlockNote( int iBlockID )
    {
    	super( iBlockID );
    	
    	setHardness( 0.8F );
    	SetAxesEffectiveOn();
    	
    	SetBuoyant();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.WOOD_BASED_BLOCK );    	

    	setUnlocalizedName( "musicBlock" );
    }
    
    @Override
    public boolean onBlockActivated( World world, int i, int j, int k, EntityPlayer player, int iFacing, float fXClick, float fYClick, float fZClick )
    {
        if ( !world.isRemote)
        {
        	// handle the player clicking on a note block with a tuning fork
        	
        	ItemStack playerItem = player.getCurrentEquippedItem();
        	
        	if ( playerItem != null && playerItem.getItem().itemID == FCBetterThanWolves.fcItemTuningFork.itemID )
        	{
                TileEntityNote tileEnt = (TileEntityNote)world.getBlockTileEntity( i, j, k );

                if ( tileEnt != null )
                {
	        		tileEnt.note = (byte)playerItem.getItemDamage();
	        		tileEnt.triggerNote( world, i, j, k );	                
                }
                
        		return true;
        	}
        }
        
        return super.onBlockActivated( world, i, j, k, player, iFacing, fXClick, fYClick, fZClick );
    }
    
    @Override
    public boolean IsIncineratedInCrucible()
    {
    	return false;
    }
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
