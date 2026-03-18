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


import java.util.List;

public class FCItemStumpRemover extends Item
{
    public FCItemStumpRemover( int iItemID )
    {
    	super( iItemID );
    	
        setMaxDamage( 0 );
        setHasSubtypes( false );
        
        maxStackSize = 16;
        
        SetBuoyant();
	    SetBellowsBlowDistance( 1 );
		SetFilterableProperties( m_iFilterable_Small );
        
    	setUnlocalizedName( "fcItemStumpRemover" );
    	
    	setCreativeTab( CreativeTabs.tabTools );
    }
    
    @Override
    public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )    
    {
		// tests copied over from itemReed to be on the safe side
        if ( player != null && !player.canPlayerEdit( i, j, k, iFacing, itemStack ) )
        {
            return false;
        }
        
        if ( itemStack.stackSize == 0 )
        {
            return false;
        }
        
		int iTargetBlockID = world.getBlockId( i, j, k );
		Block blockTarget = Block.blocksList[iTargetBlockID];
		
        if ( blockTarget != null && blockTarget.GetDoesStumpRemoverWorkOnBlock( world, i, j, k ) )
        {
        	if ( !world.isRemote )
        	{
        		world.setBlockWithNotify( i, j, k, 0 );
        		
    	        world.playAuxSFX( FCBetterThanWolves.m_iStumpRemovedAuxFXID, i, j, k, 0 );
        	}
        	
            itemStack.stackSize--;	                
            
        	return true;        	
        }
            
    	return false;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}