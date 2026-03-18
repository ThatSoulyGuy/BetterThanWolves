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


public class FCBlockBedrock extends FCBlockFullBlock
{
	public FCBlockBedrock( int iBlockID )
	{
		super( iBlockID, Material.rock );
		
		setBlockUnbreakable();
		setResistance( 6000000F );
		
		setStepSound( soundStoneFootstep );
		
		setUnlocalizedName( "bedrock" );
		
		disableStats();
		
		setCreativeTab( CreativeTabs.tabBlock );
	}

	@Override
    public int getMobilityFlag()
    {
        return 2; // cannot be pushed
    }
	
	@Override
    public boolean CanMobsSpawnOn( World world, int i, int j, int k )
    {
    	return false;
    }
	
    @Override
    public ItemStack GetStackRetrievedByBlockDispenser( World world, int i, int j, int k )
    {	 
    	return null; // can't be picked up
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
