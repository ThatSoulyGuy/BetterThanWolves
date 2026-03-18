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


import java.util.Random;

public abstract class FCBlockFarmlandLegacyBase extends FCBlockFarmlandBase
{
    public FCBlockFarmlandLegacyBase( int iBlockID )
    {
    	super( iBlockID );    	
    }
    
    @Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iNeighborBlockID )
    {
        super.onNeighborBlockChange( world, i, j, k, iNeighborBlockID );
        
        Block blockAbove = Block.blocksList[world.getBlockId( i, j + 1, k )];
        
        Material material = world.getBlockMaterial( i, j + 1, k );

        if ( blockAbove != null )
        {
	        if ( blockAbove.blockMaterial.isSolid() )
	        {
	            world.setBlockWithNotify( i, j, k, FCBetterThanWolves.fcBlockDirtLoose.blockID );
	        }
	        else if ( blockAbove.GetConvertsLegacySoil( world, i, j + 1, k ) )
	        {
	        	// the new mod crop types (like wheat) convert legacy soil when planted
	        	
	        	ConvertToNewSoil( world, i, j, k );
	        }
        }
    }

    @Override
    public boolean IsHydrated( int iMetadata )
    {
    	// stores decreasing levels of hydration from 7 to 1
    	
    	return iMetadata > 0;
    }
    
    @Override
    public int SetFullyHydrated( int iMetadata )
    {
    	return iMetadata | 7;
    }
    
	@Override
	public void DryIncrementally( World world, int i, int j, int k )
	{
        int iMetadata = world.getBlockMetadata( i, j, k );

        world.setBlockMetadataWithNotify( i, j, k, iMetadata - 1 );
	}

    //------------- Class Specific Methods ------------//
	
	public abstract void ConvertToNewSoil( World world, int i, int j, int k );
    
	//----------- Client Side Functionality -----------//
}