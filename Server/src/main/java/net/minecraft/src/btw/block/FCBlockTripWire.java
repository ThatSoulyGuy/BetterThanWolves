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


public class FCBlockTripWire extends BlockTripWire
{
    public FCBlockTripWire( int iBlockID )
    {
        super( iBlockID );
        
        InitBlockBounds( 0D, 0D, 0D, 1D, 0.15625D, 1D );
    }

    @Override
    public void onEntityCollidedWithBlock( World world, int i, int j, int k, Entity entity )
    {
    	if ( entity.CanEntityTriggerTripwire() )
    	{
    		super.onEntityCollidedWithBlock( world, i, j, k, entity );
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
        int var5 = blockAccess.getBlockMetadata(i, j, k);
        
        boolean var6 = (var5 & 4) == 4;
        boolean var7 = (var5 & 2) == 2;

        if (!var7)
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0D, 0D, 0D, 1D, 0.09375D, 1D );
        }
        else if (!var6)
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0D, 0D, 0D, 1D, 0.5D, 1D );
        }
        else
        {
        	return AxisAlignedBB.getAABBPool().getAABB(         	
        		0D, 0.0625D, 0D, 1D, 0.15625D, 1D );
        }
    }
    
	//----------- Client Side Functionality -----------//
}
