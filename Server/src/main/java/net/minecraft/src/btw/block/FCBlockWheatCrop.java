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

public class FCBlockWheatCrop extends FCBlockCropsDailyGrowth
{
    public FCBlockWheatCrop( int iBlockID )
    {
    	super( iBlockID );    	
        
        setUnlocalizedName( "fcBlockWheatCrop" );
    }
    
	@Override
    public int GetSeedItemID()
    {
        return 0;
    }

	@Override
    public int GetCropItemID()
    {
        return FCBetterThanWolves.fcItemStraw.itemID;
    }

	@Override
	public boolean IsBlockHydratedForPlantGrowthOn( World world, int i, int j, int k )
	{
		// relays hydration from soil to top block
		
    	Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];
    	
    	return blockBelow != null && 
    		blockBelow.IsBlockHydratedForPlantGrowthOn( world, i, j - 1, k );
	}
	
	@Override
    public void IncrementGrowthLevel( World world, int i, int j, int k )
    {
    	int iGrowthLevel = GetGrowthLevel( world, i, j, k );
    	
    	if ( iGrowthLevel == 6 )
    	{
    		if ( world.isAirBlock( i, j + 1, k ) )
    		{
    			// can only grow to last stage, and into top block if it is empty
    			// intentionally don't notify block below of full stage growth, as that only
    			// occurs on top block
    			
    	        SetGrowthLevel( world, i, j, k, iGrowthLevel + 1 );

    	    	int iTopMetadata = 0;    	    	
    	        Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];
    	        
    	        if ( blockBelow == null || 
    	        	!blockBelow.GetIsFertilizedForPlantGrowth( world, i, j - 1, k ) )
    	        {
    	        	iTopMetadata = FCBetterThanWolves.fcBlockWheatCrop.SetHasGrownToday( 
    	        		iTopMetadata, true );
    	        }
    	    	
        		world.setBlockAndMetadataWithNotify( i, j + 1, k, 
        			FCBetterThanWolves.fcBlockWheatCropTop.blockID, iTopMetadata );
    		}
    	}
    	else
    	{
        	super.IncrementGrowthLevel( world, i, j, k );
    	}    	
    }
    
    @Override
    public float GroundCoverRestingOnVisualOffset( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// relay offset to top block
    	
    	Block blockBelow = Block.blocksList[blockAccess.getBlockId( i, j - 1, k )];
    	
    	if ( blockBelow != null )
    	{
        	return blockBelow.GroundCoverRestingOnVisualOffset( blockAccess, i, j - 1, k );
    	}
    	
    	return 0F;
    }
    
	@Override
	public boolean GetIsFertilizedForPlantGrowth( World world, int i, int j, int k )
	{
		// relays to soil from top block
		
    	Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];
    	
    	return blockBelow != null && blockBelow.GetIsFertilizedForPlantGrowth( world, i, j - 1, k );
	}
	
	@Override
	public void NotifyOfFullStagePlantGrowthOn( World world, int i, int j, int k, Block plantBlock )
	{
		// relays to soil from top block
		
    	Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];

    	if ( blockBelow != null )
    	{
    		blockBelow.NotifyOfFullStagePlantGrowthOn( world, i, j - 1, k, plantBlock );
    	}
	}
	
    //------------- Class Specific Methods ------------//
    
    public boolean HasTopBlock( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return blockAccess.getBlockId( i, j + 1, k ) == 
    		FCBetterThanWolves.fcBlockWheatCropTop.blockID;
    }

    /**
     * Assumes the block above is a wheat top
     */
    public int GetTopBlockGrowthLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return FCBetterThanWolves.fcBlockWheatCropTop.GetGrowthLevel( blockAccess, i, j + 1, k );
    }
    
	//----------- Client Side Functionality -----------//
}
