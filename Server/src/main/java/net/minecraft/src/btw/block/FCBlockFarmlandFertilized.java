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

//FCMOD


public class FCBlockFarmlandFertilized extends FCBlockFarmland
{
    public FCBlockFarmlandFertilized( int iBlockID )
    {
    	super( iBlockID );    	
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
		
		world.setBlockAndMetadataWithNotify( i, j, k, 
			FCBetterThanWolves.fcBlockFarmland.blockID, iMetadata );
	}
	
	@Override
    public boolean IsFertilized( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
