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


public class FCItemWickerWeaving extends FCItemCraftingProgressive
{
	// takes half as long as other progressive crafting	
	static public final int m_iWickerWeavingMaxDamage = ( 60 * 20 / m_iProgressTimeInterval );
	
    public FCItemWickerWeaving( int iItemID )
    {
    	super( iItemID );
    	
        SetBuoyant();
        SetBellowsBlowDistance( 2 );
    	SetIncineratedInCrucible();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.WICKER_PIECE );
        SetFilterableProperties( Item.m_iFilterable_Thin );
    	
        setUnlocalizedName( "fcItemWickerWeaving" );        
    }
    
    @Override
    public void PlayCraftingFX( ItemStack stack, World world, EntityPlayer player )
    {
        player.playSound( "step.grass", 
        	0.25F + 0.25F * (float)world.rand.nextInt( 2 ), 
        	( world.rand.nextFloat() - world.rand.nextFloat() ) * 0.25F + 1.75F );
    }
    
    @Override
    public ItemStack onEaten( ItemStack stack, World world, EntityPlayer player )
    {
        world.playSoundAtEntity( player, "step.grass", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F );
        
        return new ItemStack( FCBetterThanWolves.fcItemWickerPiece, 1, 0 );
    }
    
    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			player.playSound( "step.grass", 1.0F, world.rand.nextFloat() * 0.1F + 0.9F );
		}
		
    	super.onCreated( stack, world, player );
    }
    
    @Override
    public boolean GetCanBeFedDirectlyIntoCampfire( int iItemDamage )
    {
    	return false;
    }
    
    @Override
    public boolean GetCanBeFedDirectlyIntoBrickOven( int iItemDamage )
    {
    	return false;
    }
    
    @Override
    public int GetProgressiveCraftingMaxDamage()
    {
    	return m_iWickerWeavingMaxDamage;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
