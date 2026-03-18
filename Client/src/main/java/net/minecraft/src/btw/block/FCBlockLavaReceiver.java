package net.minecraft.src.btw.block;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
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

public abstract class FCBlockLavaReceiver extends FCBlockMortarReceiver
{
	public static final int m_iLavaFillTickRate = 20;
	public static final int m_iLavaHardenTickRate = 2;
	
    public FCBlockLavaReceiver( int iBlockID, Material material )
    {
        super( iBlockID, material );
        
        setTickRandomly( true );
    }
    
    @Override
    public void onBlockAdded( World world, int i, int j, int k ) 
    {
    	if ( !ScheduleUpdatesForLavaAndWaterContact( world, i, j, k ) )
    	{
    		super.onBlockAdded( world, i, j, k );
    	}    	
    }
    
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand ) 
    {
    	if ( !CheckForFall( world, i, j, k ) )
    	{
	    	if ( GetHasLavaInCracks( world, i, j, k ) )
	    	{
	    		if ( HasWaterAbove( world, i, j, k ) )
	    		{
	                world.playAuxSFX( FCBetterThanWolves.m_iFireFizzSoundAuxFXID, i, j, k, 0 );
	                
	    			world.setBlockWithNotify( i, j, k, Block.stone.blockID );
	    			
	    			return;
	    		}
	    	}
	    	else if ( HasLavaAbove( world, i, j, k ) )
	    	{
	            SetHasLavaInCracks( world, i, j, k, true );
	    	}
    	}
    }
    
	@Override
    public void RandomUpdateTick( World world, int i, int j, int k, Random rand )
    {
    	if ( GetHasLavaInCracks( world, i, j, k ) )
    	{
	        if ( world.IsRainingAtPos( i, j + 1, k ) )
	        {
	        	world.playAuxSFX( FCBetterThanWolves.m_iFireFizzSoundAuxFXID, i, j, k, 0 );
            
	        	world.setBlockWithNotify( i, j, k, Block.stone.blockID );
	        }
    	}
    }
	
    @Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iNeighborBlockID ) 
    {
    	if ( !ScheduleUpdatesForLavaAndWaterContact( world, i, j, k ) )
    	{    	
    		super.onNeighborBlockChange( world, i, j, k, iNeighborBlockID );
    	}
    }
    
    @Override
    public boolean canSilkHarvest()
    {
    	// prevent havest of version with lava in cracks
    	
        return false;
    }
    
    @Override
    public boolean GetIsBlockWarm( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return GetHasLavaInCracks( blockAccess, i, j, k );
    }
    
    //------------- Class Specific Methods ------------//
    
    public boolean GetHasLavaInCracks( int iMetadata )
    {
    	return ( iMetadata & 1 ) != 0;
    }
    
    public boolean GetHasLavaInCracks( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return GetHasLavaInCracks( blockAccess.getBlockMetadata( i, j, k ) );
    }
    
    public int SetHasLavaInCracks( int iMetadata, boolean bHasLava )
    {
    	if ( bHasLava )
    	{
    		iMetadata |= 1;
    	}
    	else
    	{
    		iMetadata &= (~1);
    	}
    	
    	return iMetadata;
    }
    
    public void SetHasLavaInCracks( World world, int i, int j, int k, boolean bHasLava )
    {
    	int iMetadata =  SetHasLavaInCracks( world.getBlockMetadata( i, j, k ), bHasLava );
    	
    	world.setBlockMetadataWithNotify( i, j, k, iMetadata );
    }    	
    
    public boolean HasLavaAbove( IBlockAccess blockAccess, int i, int j, int k )
    {
		Block blockAbove = Block.blocksList[blockAccess.getBlockId( i, j + 1, k )];
		
		return blockAbove != null && blockAbove.blockMaterial == Material.lava;
    }
    
    public boolean HasWaterAbove( IBlockAccess blockAccess, int i, int j, int k )
    {
		Block blockAbove = Block.blocksList[blockAccess.getBlockId( i, j + 1, k )];
		
		return blockAbove != null && blockAbove.blockMaterial == Material.water;
    }
    
    public boolean ScheduleUpdatesForLavaAndWaterContact( World world, int i, int j, int k )
    {
    	if ( GetHasLavaInCracks( world, i, j, k ) )
    	{
    		if ( HasWaterAbove( world, i, j, k ) )
    		{
    			if ( !world.IsUpdatePendingThisTickForBlock( i, j, k, blockID ) )
    			{
    				world.scheduleBlockUpdate( i, j, k, blockID, m_iLavaHardenTickRate );
    			}
                
                return true;
    		}
    	}
    	else if ( HasLavaAbove( world, i, j, k ) )
    	{
			if ( !world.IsUpdatePendingThisTickForBlock( i, j, k, blockID ) )
			{
				world.scheduleBlockUpdate( i, j, k, blockID, m_iLavaFillTickRate );
			}
            
            return true;
    	}
    	
    	return false;
    }
    
    //------------ Client Side Functionality ----------//
    
    public abstract Icon GetLavaCracksOverlay();
    
    @Override
    public void RenderBlockSecondPass( RenderBlocks renderBlocks, int i, int j, int k, boolean bFirstPassResult )
    {
    	if ( bFirstPassResult && GetHasLavaInCracks( renderBlocks.blockAccess, i, j, k ) )
    	{
	        FCClientUtilsRender.RenderBlockFullBrightWithTexture( renderBlocks, 
	        	renderBlocks.blockAccess, i, j, k, GetLavaCracksOverlay() );
    	}
    }
    
    @Override
    public void RenderFallingBlock( RenderBlocks renderBlocks, int i, int j, int k, int iMetadata )
    {
    	renderBlocks.SetRenderAllFaces( true );
    	
        renderBlocks.setRenderBounds( GetFixedBlockBoundsFromPool() );
        
        renderBlocks.renderStandardBlock( this, i, j, k );
        
    	if ( GetHasLavaInCracks( iMetadata ) )
    	{
    		FCClientUtilsRender.RenderBlockFullBrightWithTexture( renderBlocks, 
    			renderBlocks.blockAccess, i, j, k, GetLavaCracksOverlay() );
    	}
        
    	renderBlocks.SetRenderAllFaces( false );
    }
}