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


public class FCBlockFarmlandLegacyUnfertilized extends FCBlockFarmlandLegacyBase
{
    public FCBlockFarmlandLegacyUnfertilized( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName( "fcBlockFarmlandFertilizedNew" );
    }
    
	@Override
    public boolean GetCanGrassSpreadToBlock( World world, int i, int j, int k )
    {
    	return world.isAirBlock( i, j + 1, k );
    }
    
	@Override
    public boolean AttempToSpreadGrassToBlock( World world, int i, int j, int k )
    {
    	if ( world.rand.nextInt( 3 ) == 0 )
    	{
    		return super.AttempToSpreadGrassToBlock( world, i, j, k );
    	}
    	
    	return false;
    }
    
	@Override
    public boolean SpreadGrassToBlock( World world, int i, int j, int k )
    {
		world.setBlockAndMetadataWithNotify( i, j + 1, k, Block.tallGrass.blockID, 1 );
		
    	return true;
    }
    
	@Override
    public boolean IsFertilized( IBlockAccess blockAccess, int i, int j, int k )
	{
		return false;
	}
    
	@Override
    public void SetFertilized( World world, int i, int j, int k )
    {
    	int iTargetBlockMetadata = world.getBlockMetadata( i, j, k );
    	
    	world.setBlockAndMetadataWithNotify( i, j, k, 
    		FCBetterThanWolves.fcBlockFarmlandLegacyFertilized.blockID, iTargetBlockMetadata );
    }
    
	@Override
	public void ConvertToNewSoil( World world, int i, int j, int k )
	{
		int iNewMetadata = 0;
		
		if ( IsHydrated( world, i, j, k ) )
		{
			iNewMetadata = FCBetterThanWolves.fcBlockFarmland.SetFullyHydrated( iNewMetadata );
		}
		
    	world.setBlockAndMetadataWithNotify( i, j, k, 
    		FCBetterThanWolves.fcBlockFarmland.blockID, iNewMetadata );
	}
	
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
