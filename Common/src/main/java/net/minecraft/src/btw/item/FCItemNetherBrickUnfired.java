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


public class FCItemNetherBrickUnfired extends FCItemPlacesAsBlock
{
    public FCItemNetherBrickUnfired( int iItemID )
    {
    	super( iItemID, FCBetterThanWolves.fcUnfiredPottery.blockID, 
    		FCBlockUnfiredPottery.m_iSubtypeNetherBrick );
    	
    	SetNeutralBuoyant();
    	
    	setUnlocalizedName( "fcItemBrickNetherUnfired" );
    	
    	setCreativeTab( CreativeTabs.tabMaterials );
    }
    
    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			player.playSound( "mob.slime.attack", 0.25F, 
				(  world.rand.nextFloat() - world.rand.nextFloat() ) * 0.1F + 0.7F );
		}
		
		super.onCreated( stack, world, player );
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
