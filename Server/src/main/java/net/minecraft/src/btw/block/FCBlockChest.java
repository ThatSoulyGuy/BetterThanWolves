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


public class FCBlockChest extends BlockChest
{
    public FCBlockChest( int iBlockID )
    {
    	super( iBlockID, 0 );
    	
    	SetBlockMaterial( FCBetterThanWolves.fcMaterialPlanks );
    	
    	setHardness( 1.5F );    	
    	SetAxesEffectiveOn();
    	
    	SetBuoyant();    	
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.WOOD_BASED_BLOCK );
    	
        InitBlockBounds( 0.0625F, 0F, 0.0625F, 0.9375F, 0.875F, 0.9375F );
        
    	setStepSound( soundWoodFootstep );
    	
    	setUnlocalizedName( "chest" );    	
    }
    
    @Override
    public TileEntity createNewTileEntity( World world )
    {
        return new FCTileEntityChest();
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
        if ( blockAccess.getBlockId( i, j, k - 1 ) == blockID )
        {
        	return AxisAlignedBB.getAABBPool().getAABB( 
        		0.0625F, 0F, 0F, 
        		0.9375F, 0.875F, 0.9375F );
        }
        else if ( blockAccess.getBlockId( i, j, k + 1 ) == blockID )
        {
        	return AxisAlignedBB.getAABBPool().getAABB( 
        		0.0625F, 0.0F, 0.0625F, 
        		0.9375F, 0.875F, 1.0F );
        }
        else if ( blockAccess.getBlockId( i - 1, j, k ) == blockID )
        {
        	return AxisAlignedBB.getAABBPool().getAABB( 
        		0F, 0F, 0.0625F, 
        		0.9375F, 0.875F, 0.9375F );
        }
        else if ( blockAccess.getBlockId( i + 1, j, k ) == blockID )
        {
        	return AxisAlignedBB.getAABBPool().getAABB( 
        		0.0625F, 0F, 0.0625F, 
        		1F, 0.875F, 0.9375F );
        }
        else
        {
        	return AxisAlignedBB.getAABBPool().getAABB(
        		0.0625F, 0F, 0.0625F, 
        		0.9375F, 0.875F, 0.9375F );
        }        
    }
    
	@Override
    public boolean canSilkHarvest( int iMetadata )
    {
        return true;
    }    
    
    @Override
    public int GetHarvestToolLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 2; // iron or better
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemSawDust.itemID, 6, 0, fChanceOfDrop );
		DropItemsIndividualy( world, i, j, k, Item.stick.itemID, 2, 0, fChanceOfDrop );
		
		return true;
	}
	
	@Override
	public boolean CanRotateOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		// don't rotate if there's a neigboring chest
		
        if ( blockAccess.getBlockId( i - 1, j, k ) == blockID ||
        	blockAccess.getBlockId( i + 1, j, k ) == blockID ||
        	blockAccess.getBlockId( i, j, k - 1 ) == blockID ||
        	blockAccess.getBlockId( i, j, k + 1 ) == blockID )
        {
        	return false;
        }
        
        return true;
	}
	
	@Override
	public int RotateMetadataAroundJAxis( int iMetadata, boolean bReverse )
	{
		return Block.RotateFacingAroundJ( iMetadata, bReverse );
	}


	@Override
    public boolean CanSupportFallingBlocks( IBlockAccess blockAccess, int i, int j, int k )
    {
		// this is to prevent sand collapsing during worldgen
		
		return true;
    }
    
	//----------- Client Side Functionality -----------//
}
