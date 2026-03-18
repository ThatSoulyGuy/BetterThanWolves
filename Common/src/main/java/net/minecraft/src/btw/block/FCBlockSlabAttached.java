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


public abstract class FCBlockSlabAttached extends FCBlockSlab
{
    public FCBlockSlabAttached( int iBlockID, Material material )
    {
        super( iBlockID, material );
    }
    
    @Override
    public boolean canPlaceBlockOnSide( World world, int i, int j, int k, int iSide )
    {
    	if ( iSide == 0 || iSide == 1 )
    	{
    		if ( HasValidAnchorToFacing( world, i, j, k, Block.GetOppositeFacing( iSide ) ) )
			{
		        return super.canPlaceBlockOnSide( world, i, j, k, iSide );
			}
    	}
    	else if ( HasValidAnchorToFacing( world, i, j, k, 0 ) || 
			HasValidAnchorToFacing( world, i, j, k, 1 ) )
		{
	        return super.canPlaceBlockOnSide( world, i, j, k, iSide );
		}
    	
		return false;
    }
    
	@Override
    public int onBlockPlaced( World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ, int iMetadata )
    {
		if ( iFacing == 0 )
		{
        	iMetadata = SetIsUpsideDown( iMetadata, true );
		}
		else if ( iFacing != 1 )
        {
			if ( (double)fClickY > 0.5D )
			{
				if ( HasValidAnchorToFacing( world, i, j, k, 1 ) )
				{
		        	iMetadata = SetIsUpsideDown( iMetadata, true );
				}
			}
			else
			{
				if ( !HasValidAnchorToFacing( world, i, j, k, 0 ) )
				{
		        	iMetadata = SetIsUpsideDown( iMetadata, true );
				}
			}
        }
        
        return iMetadata;
    }
	
	@Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iNeighborBlockID )
    {
		int iAnchorSide = 0;
		
		if ( GetIsUpsideDown( world.getBlockMetadata( i, j, k ) ) )
		{
			iAnchorSide = 1;			
		}
		
		if ( !HasValidAnchorToFacing( world, i, j, k, iAnchorSide ) )
		{
			OnAnchorBlockLost( world, i, j, k );
			
	        world.setBlockToAir( i, j, k );
		}
    }
	
    //------------- Class Specific Methods ------------//    
    
	public boolean HasValidAnchorToFacing( World world, int i, int j, int k, int iFacing )
	{
		FCUtilsBlockPos attachedPos = new FCUtilsBlockPos( i, j, k, iFacing );
		
		return FCUtilsWorld.DoesBlockHaveLargeCenterHardpointToFacing( world, attachedPos.i, attachedPos.j, attachedPos.k, 
			Block.GetOppositeFacing( iFacing ), true );		
	}
	
	abstract protected void OnAnchorBlockLost( World world, int i, int j, int k );
	
	//----------- Client Side Functionality -----------//
}
