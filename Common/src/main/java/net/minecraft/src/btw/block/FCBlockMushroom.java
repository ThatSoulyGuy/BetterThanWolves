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

public class FCBlockMushroom extends BlockMushroom
{
    public FCBlockMushroom( int iBlockID, String iconName )
    {
    	super( iBlockID, iconName );
        
        InitBlockBounds( 0.3D, 0D, 0.3D, 0.7D, 0.4D, 0.7D );
        
        setCreativeTab( null );
    }
    
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
    	if ( world.provider.dimensionId != 1 )
    	{
	        if ( world.getBlockId( i, j - 1, k ) == Block.mycelium.blockID && 
	        	rand.nextInt( 50 ) == 0 )
	        {
	        	// mushrooms growing on mycelium have a chance of sprouting into giant mushrooms
	        	
	    		fertilizeMushroom( world, i, j, k, rand );
	        }
	        else
	        {        
	        	super.updateTick( world, i, j, k, rand );
	        }
    	}
    }
    
    @Override
    public boolean canPlaceBlockAt( World world, int i, int j, int k )
    {
    	// avoid light level tests in BlockFlower
    	
        int iBlockID = world.getBlockId( i, j, k );
        
        return ( iBlockID == 0 || blocksList[iBlockID].blockMaterial.isReplaceable() ) && 
        	canBlockStay( world, i, j, k );
    }
    
    @Override
    public boolean canBlockStay( World world, int i, int j, int k )
    {
        int iBlockBelowID = world.getBlockId( i, j - 1, k );
    
        return iBlockBelowID == Block.mycelium.blockID || 
        	( world.getFullBlockLightValue( i, j, k ) < 13 && 
			CanGrowOnBlock( world, i, j - 1, k ) );
    }
    
    @Override
    public int idDropped( int iMetaData, Random rand, int iFortuneModifier )
    {
		return FCBetterThanWolves.fcItemMushroomRed.itemID;
    }
    
    @Override
    public boolean CanGrowOnBlock( World world, int i, int j, int k )
    {
		return world.doesBlockHaveSolidTopSurface( i, j, k );
    }
    
	@Override
    public boolean CanBeCrushedByFallingEntity( World world, int i, int j, int k, 
    	EntityFallingSand entity )
    {
    	return true;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}