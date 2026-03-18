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

public abstract class FCBlockCropsDailyGrowth extends FCBlockCrops
{
    public  FCBlockCropsDailyGrowth( int iBlockID )
    {
        super( iBlockID );
    }
    
    @Override
    public float GetBaseGrowthChance( World world, int i, int j, int k )
    {
    	return 0.4F;
    }
    
    @Override
    public void AttemptToGrow( World world, int i, int j, int k, Random rand )
    {
        int iTimeOfDay = (int)( world.worldInfo.getWorldTime() % 24000L );
        
        if ( iTimeOfDay > 14000 && iTimeOfDay < 22000 )
        {
        	// night

        	if ( GetHasGrownToday( world, i, j, k ) )
        	{
        		SetHasGrownToday( world, i, j, k, false );
        	}
        }
        else
        {    	
	    	if ( !GetHasGrownToday( world, i, j, k ) && 
	    		GetWeedsGrowthLevel( world, i, j, k ) == 0 &&  
	    		world.GetBlockNaturalLightValue( i, j + 1, k ) >= GetLightLevelForGrowth() )
		    {
		        Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];
		        
		        if ( blockBelow != null && 
		        	blockBelow.IsBlockHydratedForPlantGrowthOn( world, i, j - 1, k ) )
		        {
		    		float fGrowthChance = GetBaseGrowthChance( world, i, j, k );
		    		
			    	if ( blockBelow.GetIsFertilizedForPlantGrowth( world, i, j - 1, k ) )
			    	{
			    		fGrowthChance *= 2F;
			    	}
			    	
		            if ( rand.nextFloat() <= fGrowthChance )
		            {
		            	IncrementGrowthLevel( world, i, j, k );
		            	
		            	UpdateFlagForGrownToday( world, i, j, k );
		            }
		        }
		    }
        }
    }
    
    //------------- Class Specific Methods ------------//
    
	public void UpdateFlagForGrownToday( World world, int i, int j, int k )
	{
    	// fertilized crops can grow twice in a day
		
        Block blockBelow = Block.blocksList[world.getBlockId( i, j - 1, k )];
        
        if ( blockBelow != null )
        {
	    	if ( !blockBelow.GetIsFertilizedForPlantGrowth( world, i, j - 1, k ) ||
	    		GetGrowthLevel( world, i, j, k ) % 2 == 0 )
	    	{
	    		SetHasGrownToday( world, i, j, k, true );
	    	}
        }
	}
	
    public boolean GetHasGrownToday( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return GetHasGrownToday( blockAccess.getBlockMetadata( i, j, k ) );
    }
    
    public boolean GetHasGrownToday( int iMetadata )
    {
    	return ( iMetadata & 8 ) != 0;
    }
    
    public void SetHasGrownToday( World world, int i, int j, int k, boolean bHasGrown )
    {
    	int iMetadata = SetHasGrownToday( world.getBlockMetadata( i, j, k ), bHasGrown );
    	
    	// intentionally no notify as this does not affect the visual state and should
    	// not trigger Buddy
    	
    	world.setBlockMetadata( i, j, k, iMetadata );
    }
    
    public int SetHasGrownToday( int iMetadata, boolean bHasGrown )
    {
    	if ( bHasGrown )
    	{
    		iMetadata |= 8;
    	}
    	else
    	{
    		iMetadata &= (~8);
    	}
    	
    	return iMetadata;
    }
    
	//----------- Client Side Functionality -----------//
}
