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


public class FCBlockPressurePlate extends BlockPressurePlate
{
	public static final double m_dHorizontalBorder = 0.0625D;
	public static final double m_dHeightDepressed = 0.03125D;
	public static final double m_dHeightResting = 0.0625D;
	
	public static final double m_dHeightItem = 0.25D;
	public static final double m_dHalfHeightItem = ( m_dHeightItem / 2D );

    public FCBlockPressurePlate( int iBlockID, String iconName, Material material, EnumMobType mobType )
    {
    	super( iBlockID, iconName, material, mobType );
    	
        InitBlockBounds( m_dHorizontalBorder, 0D, m_dHorizontalBorder, 
        	1D - m_dHorizontalBorder, m_dHeightDepressed, 1D - m_dHorizontalBorder );
    }

    @Override
    public boolean canPlaceBlockAt( World world, int i, int j, int k )
    {
    	// override to prevent placing pressure plates on fences
    	
    	return world.doesBlockHaveSolidTopSurface( i, j - 1, k );    
	}

	@Override
    public void setBlockBoundsBasedOnState( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// override to deprecate parent
    }
	
	@Override
    public void func_94353_c_(int par1)
    {
    	// override to deprecate parent
    }
    
	@Override
    public void setBlockBoundsForItemRender()
    {
    	// override to deprecate parent
    }
    
    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState( 
    	IBlockAccess blockAccess, int i, int j, int k )
    {
        boolean bDepressed = getPowerSupply( blockAccess.getBlockMetadata( i, j, k ) ) > 0;

        if ( bDepressed )
        {
            return AxisAlignedBB.getAABBPool().getAABB( 
            	m_dHorizontalBorder, 0D, m_dHorizontalBorder, 
            	1D - m_dHorizontalBorder, m_dHeightDepressed, 1D - m_dHorizontalBorder );
        }
        else
        {
            return AxisAlignedBB.getAABBPool().getAABB( 
            	m_dHorizontalBorder, 0D, m_dHorizontalBorder, 
            	1D - m_dHorizontalBorder, m_dHeightResting, 1D - m_dHorizontalBorder );
        }
    }
    
    @Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iNeighborBlockID )
    {
		// override to prevent pressure plates on fences and because this portion of the vanilla code seems to have the potential for item duplication
		
        boolean bOnInvalidSurface = false;

        if ( !world.doesBlockHaveSolidTopSurface( i, j - 1, k ) )
        {
            bOnInvalidSurface = true;
        }

        if ( bOnInvalidSurface )
        {
        	if ( world.getBlockId( i, j, k ) == blockID )
        	{
	            dropBlockAsItem( world, i, j, k, world.getBlockMetadata( i, j, k ), 0 );
	            world.setBlockToAir( i, j, k );
        	}
        }
    }
    
    //------------- Class Specific Methods ------------//
    
    private boolean IsModFence( World world, int i, int j, int k )
    {
    	int iBlockID = world.getBlockId( i, j, k );
    	
    	Block block = Block.blocksList[iBlockID];
    	
    	if ( block != null )
    	{
    		if ( block instanceof FCBlockSidingAndCornerAndDecorative )
    		{
    	    	int iSubtype = world.getBlockMetadata( i, j, k );
    	    	
    	    	return iSubtype == FCBlockSidingAndCornerAndDecorative.m_iSubtypeFence;    	    	
    		}
    	}
    	
    	return false;
    }
    
	//----------- Client Side Functionality -----------//

    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolForItemRender( int iItemDamage )
    {
        return AxisAlignedBB.getAABBPool().getAABB( 0D, 0.5D - m_dHalfHeightItem, 0D,
        	1D, 0.5D + m_dHalfHeightItem, 1D );
    }
    
    @Override
    public boolean shouldSideBeRendered( IBlockAccess blockAccess, int i, int j, int k, int iSide )
    {
    	if ( iSide == 0 )
    	{
    		return !blockAccess.isBlockOpaqueCube( i, j, k );
    	}
    	
    	return true;
    }
}