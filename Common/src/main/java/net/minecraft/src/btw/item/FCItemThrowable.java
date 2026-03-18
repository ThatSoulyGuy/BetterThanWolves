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


public abstract class FCItemThrowable extends Item
{
    public FCItemThrowable( int iItemID )
    {
    	super( iItemID );
    }
    
    @Override
    public ItemStack onItemRightClick( ItemStack stack, World world, EntityPlayer player )
    {        
        if ( !player.capabilities.isCreativeMode )
        {
        	stack.stackSize--;
        }
        
        world.playSoundAtEntity( player, "random.bow", 0.5F, 0.4F / 
        	( itemRand.nextFloat() * 0.4F + 0.8F ) );
        
        if( !world.isRemote )
        {
        	SpawnThrownEntity( stack, world, player );
        }
    	
        return stack;
    }
    
    @Override
	public boolean OnItemUsedByBlockDispenser( ItemStack stack, World world, 
		int i, int j, int k, int iFacing )
	{
        FCUtilsBlockPos offsetPos = new FCUtilsBlockPos( 0, 0, 0, iFacing );
        
        double dXPos = i + ( offsetPos.i * 0.6D ) + 0.5D;
        double dYPos = j + ( offsetPos.j * 0.6D ) + 0.5D;
        double dZPos = k + ( offsetPos.k * 0.6D ) + 0.5D;
    	
    	double dYHeading;
    	
    	if ( iFacing > 2 )
    	{
    		// slight upwards trajectory when fired sideways
    		
    		dYHeading = 0.1D;
    	}
    	else
    	{
    		dYHeading = offsetPos.j;
    	}
    	
    	EntityThrowable entity = GetEntityFiredByByBlockDispenser( world, dXPos, dYPos, dZPos );
    	
        entity.setThrowableHeading( offsetPos.i, dYHeading, offsetPos.k, 1.1F, 6F );
        
        world.spawnEntityInWorld( entity );
        
        world.playAuxSFX( 1002, i, j, k, 0 ); // bow sound
        
		return true;
	}
    
    //------------- Class Specific Methods ------------//
    
    public abstract void SpawnThrownEntity( ItemStack stack, World world, 
    	EntityPlayer player );
    
    public abstract EntityThrowable GetEntityFiredByByBlockDispenser( World world, 
    	double dXPos, double dYPos, double dZPos );
    
	//------------ Client Side Functionality ----------//
}
