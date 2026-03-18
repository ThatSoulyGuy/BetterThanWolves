package net.minecraft.src.btw.item;

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


public class FCItemMortar extends Item
{
    public FCItemMortar( int iItemID )
    {
    	super( iItemID );
    }
    
    @Override
    public boolean onItemUse( ItemStack stack, EntityPlayer player, World world, 
    	int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
        if ( player != null && player.canPlayerEdit( i, j, k, iFacing, stack ) )
        {
        	Block targetBlock = Block.blocksList[world.getBlockId( i, j, k )];
        	
        	if ( targetBlock != null && targetBlock.OnMortarApplied( world, i, j, k ) )
        	{            	
    			if ( !world.isRemote )
    			{
    		        world.playAuxSFX( FCBetterThanWolves.m_iMortarAppliedAuxFXID, i, j, k, 0 ); 
    			}
    	        
    			stack.stackSize--;
    			
            	return true;
        	}
        }
        
        return false;
    }
    
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
