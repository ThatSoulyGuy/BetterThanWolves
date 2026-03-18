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

public class FCBlockLadderBase extends Block
{
    public static final float m_fLadderThickness = 0.125F;

    public static final AxisAlignedBB m_boxCollision = new AxisAlignedBB( 0D, 0D, 1D - m_fLadderThickness, 1D, 1D, 1D );
    
    public FCBlockLadderBase( int iBlockID )
    {
        super( iBlockID, Material.circuits );
        
        setHardness( 0.4F );        
        SetAxesEffectiveOn( true );
        
        SetBuoyant();
        
        setStepSound( soundLadderFootstep );
    }
    
    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return FCBetterThanWolves.fcBlockLadder.blockID;
    }
    
    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState( 
    	IBlockAccess blockAccess, int i, int j, int k )
    {
    	AxisAlignedBB transformedBox = m_boxCollision.MakeTemporaryCopy();
    	
    	transformedBox.RotateAroundJToFacing( GetFacing( blockAccess, i, j, k ) );
    	
        return transformedBox;        
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt( World world, int i, int j, int k )
    {
    	for ( int iTempFacing = 2; iTempFacing <= 5; iTempFacing++ )
    	{
    		if ( CanAttachToFacing( world, i, j, k, Block.GetOppositeFacing( iTempFacing ) ) )
    		{    			
    			return true;
    		}
    	}
    	
    	return false;
    }

    @Override
    public int onBlockPlaced( World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ, int iMetadata )
    {
    	if ( CanAttachToFacing( world, i, j, k, Block.GetOppositeFacing( iFacing ) ) )
		{
    		iMetadata = SetFacing( iMetadata, iFacing );
		}
    	else
    	{    	
    		// specified facing isn't valid, search for another
    		
	    	for ( int iTempFacing = 2; iTempFacing <= 5; iTempFacing++ )
	    	{
	    		if ( CanAttachToFacing( world, i, j, k, iTempFacing ) )
    			{
	        		iMetadata = SetFacing( iMetadata, Block.GetOppositeFacing( iTempFacing ) );
	        		
	        		break;
    			}
	    	}
    	}
    	
        return iMetadata;
    }
    
    @Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iChangedBlockID )
    {
        int iMetadata = world.getBlockMetadata( i, j, k );
        
		if ( !CanAttachToFacing( world, i, j, k, Block.GetOppositeFacing( GetFacing( iMetadata ) ) ) )
		{
            dropBlockAsItem( world, i, j, k, iMetadata, 0 );
            world.setBlockToAir( i, j, k );
		}
			
        super.onNeighborBlockChange( world, i, j, k, iChangedBlockID );
    }
    
    @Override
    public int getRenderType()
    {
        return 8;
    }

	@Override
	public boolean IsBlockClimbable( World world, int i, int j, int k )
	{
		return true;
	}
	
	@Override
    public int GetFacing( int iMetadata )
    {
    	return ( iMetadata & 3 ) + 2;
    }
    
	@Override
	public int SetFacing( int iMetadata, int iFacing )
    {
    	int iFlatFacing = MathHelper.clamp_int( iFacing, 2, 5 ) - 2;
    	
    	iMetadata &= ~3;
    	
    	return iMetadata | iFlatFacing;
    }
    
	@Override
    public boolean CanRotateAroundBlockOnTurntableToFacing( World world, int i, int j, int k, int iFacing  )
    {
		return iFacing == Block.GetOppositeFacing( GetFacing( world, i, j, k ) );
    }
    
	@Override
    public int GetNewMetadataRotatedAroundBlockOnTurntableToFacing( World world, int i, int j, int k, int iInitialFacing, int iRotatedFacing )
    {
		int iOldMetadata = world.getBlockMetadata( i, j, k );
		
		return SetFacing( iOldMetadata, Block.GetOppositeFacing( iRotatedFacing ) );
    }
	
    @Override
    public boolean CanItemPassIfFilter( ItemStack filteredItem )
    {
    	int iFilterableProperties = filteredItem.getItem().GetFilterableProperties( filteredItem ); 
    		
    	return ( iFilterableProperties & Item.m_iFilterable_SolidBlock ) == 0;
    }
    
    @Override
    public boolean CanMobsSpawnOn( World world, int i, int j, int k )
    {
    	return false;
    }
    
    //------------- Class Specific Methods ------------//
    
    public boolean CanAttachToFacing( World world, int i, int j, int k, int iFacing )
    {
    	if ( iFacing >= 2 )
    	{
    		FCUtilsBlockPos targetPos = new FCUtilsBlockPos( i, j, k, iFacing );
    		
    		return FCUtilsWorld.DoesBlockHaveLargeCenterHardpointToFacing( world, targetPos.i, targetPos.j, targetPos.k, 
    			Block.GetOppositeFacing( iFacing ) );
    	}
    	
    	return false;
    }
    
	//----------- Client Side Functionality -----------//
}
