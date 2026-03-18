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

public class FCBlockFarmlandLegacyFertilized extends FCBlockFarmlandLegacyBase
{
	private static final int m_iDefaultTexture = 2;
	private static final int m_iTopWetTexture = 136;
	public static final int m_iTopDryTexture = 137;
	
    public FCBlockFarmlandLegacyFertilized( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName( "FCBlockFarmlandFertilized" );
    }
    
	@Override
	public float GetPlantGrowthOnMultiplier( World world, int i, int j, int k, Block plantBlock )
	{
		return 2F;
	}
	
	@Override
	public boolean GetIsFertilizedForPlantGrowth( World world, int i, int j, int k )
	{
		return true;
	}
	
	@Override
	public void NotifyOfFullStagePlantGrowthOn( World world, int i, int j, int k, Block plantBlock )
	{
		// revert back to unfertilized soil
		
		int iMetadata = world.getBlockMetadata( i, j, k );
		
		world.setBlockAndMetadataWithNotify( i, j, k, Block.tilledField.blockID, iMetadata );
	}
	
	@Override
    public boolean IsFertilized( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
    
	@Override
	public void ConvertToNewSoil( World world, int i, int j, int k )
	{
		int iNewMetadata = 0;
		
		if ( IsHydrated( world, i, j, k ) )
		{
			iNewMetadata = FCBetterThanWolves.fcBlockFarmlandFertilized.SetFullyHydrated( 
				iNewMetadata );
		}
		
    	world.setBlockAndMetadataWithNotify( i, j, k, 
    		FCBetterThanWolves.fcBlockFarmlandFertilized.blockID, iNewMetadata );
	}
	
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//    
    
	@Override
    public void registerIcons( IconRegister register )
    {
		blockIcon = register.registerIcon( "dirt" );
		
        m_iconTopWet = register.registerIcon( "FCBlockFarmlandFertilized_wet" );
        m_iconTopDry = register.registerIcon( "FCBlockFarmlandFertilized_dry" );
    }
}