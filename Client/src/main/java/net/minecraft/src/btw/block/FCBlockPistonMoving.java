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


public class FCBlockPistonMoving extends BlockPistonMoving
{
    public FCBlockPistonMoving( int iBlockID )
    {
        super( iBlockID );
    }
    
    @Override
    public AxisAlignedBB getAxisAlignedBB( World world, int i, int j, int k, int iBlockID, float fPistonPushProgress, int iOrientation )
    {
    	// override to fix problem with piston heads moving through items
    	
        if ( iBlockID != 0 && iBlockID != this.blockID )
        {
            AxisAlignedBB boundingBox = Block.blocksList[iBlockID].GetAsPistonMovingBoundingBox(
            	world, i, j, k );

            if (boundingBox == null)
            {
                return null;
            }
            else
            {
                if (Facing.offsetsXForSide[iOrientation] < 0)
                {
                    boundingBox.minX -= (double)((float)Facing.offsetsXForSide[iOrientation] * fPistonPushProgress);
                }
                else
                {
                    boundingBox.maxX -= (double)((float)Facing.offsetsXForSide[iOrientation] * fPistonPushProgress);
                }

                if (Facing.offsetsYForSide[iOrientation] < 0)
                {
                    boundingBox.minY -= (double)((float)Facing.offsetsYForSide[iOrientation] * fPistonPushProgress);
                }
                else
                {
                    boundingBox.maxY -= (double)((float)Facing.offsetsYForSide[iOrientation] * fPistonPushProgress);
                }

                if (Facing.offsetsZForSide[iOrientation] < 0)
                {
                    boundingBox.minZ -= (double)((float)Facing.offsetsZForSide[iOrientation] * fPistonPushProgress);
                }
                else
                {
                    boundingBox.maxZ -= (double)((float)Facing.offsetsZForSide[iOrientation] * fPistonPushProgress);
                }

                return boundingBox;
            }
        }
        else
        {
            return null;
        }
    }
    
	@Override
    public void setBlockBoundsBasedOnState( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// override to deprecate parent
    }
	
    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState( 
    	IBlockAccess blockAccess, int i, int j, int k )
    {
        TileEntity tileEntity = blockAccess.getBlockTileEntity( i, j, k );

        if ( tileEntity != null && tileEntity instanceof TileEntityPiston )
        {
        	TileEntityPiston pistonEntity = (TileEntityPiston)tileEntity;
        	
            Block block = Block.blocksList[pistonEntity.getStoredBlockID()];

            if ( block != null || block != this )
            {
	            AxisAlignedBB bounds = block.GetBlockBoundsFromPoolBasedOnState(
	            	blockAccess, i, j, k );
	            
	            float fExtensionRatio = pistonEntity.getProgress(0.0F);
	
	            if (pistonEntity.isExtending())
	            {
	                fExtensionRatio = 1.0F - fExtensionRatio;
	            }
	
	            int iFacing = pistonEntity.getPistonOrientation();
	            
	            bounds.minX -= Facing.offsetsXForSide[iFacing] * fExtensionRatio;
	            bounds.minY -= Facing.offsetsYForSide[iFacing] * fExtensionRatio;
	            bounds.minZ -= Facing.offsetsZForSide[iFacing] * fExtensionRatio;
	            
	            bounds.maxX -= Facing.offsetsXForSide[iFacing] * fExtensionRatio;
	            bounds.maxY -= Facing.offsetsYForSide[iFacing] * fExtensionRatio;
	            bounds.maxZ -= Facing.offsetsZForSide[iFacing] * fExtensionRatio;
	            
	            return bounds;
            }
        }
        
        return super.GetBlockBoundsFromPoolBasedOnState( blockAccess, i, j, k );
    }

    @Override
    public boolean CanSupportFallingBlocks( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// wait until after the piston stops moving to evaluate falling blocks
    	
    	return true;    	
    }
    
    @Override
    public ItemStack GetStackRetrievedByBlockDispenser( World world, int i, int j, int k )
    {	 
    	return null; // can't be picked up
    }
    
	//------------- Class Specific Methods ------------//
	
    public static TileEntity GetShoveledTileEntity( int iBlockID, int iMetadata, int iFacing )
    {
        return new TileEntityPiston( iBlockID, iMetadata, iFacing, true, false, true );
    }
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
    	// rendering handled by tile entity
    	
    	return false;
    }
}
