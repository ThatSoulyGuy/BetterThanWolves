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


public class FCBlockTripWireSource extends BlockTripWireSource
{
    public FCBlockTripWireSource( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName("tripWireSource");        
        
        setCreativeTab( null );        
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
        int iDirection = blockAccess.getBlockMetadata(i, j, k) & 3;
        float fHalfWidth = 0.1875F;

        if ( iDirection == 3 )
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0.0F, 0.2F, 0.5F - fHalfWidth, fHalfWidth * 2.0F, 0.8F, 0.5F + fHalfWidth );
        }
        else if ( iDirection == 1 )
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		1.0F - fHalfWidth * 2.0F, 0.2F, 0.5F - fHalfWidth, 1.0F, 0.8F, 0.5F + fHalfWidth );
        }
        else if ( iDirection == 0 )
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0.5F - fHalfWidth, 0.2F, 0.0F, 0.5F + fHalfWidth, 0.8F, fHalfWidth * 2.0F );
        }
        else // == 2
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0.5F - fHalfWidth, 0.2F, 1.0F - fHalfWidth * 2.0F, 0.5F + fHalfWidth, 0.8F, 1.0F );
        }
    }
    
	//------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}