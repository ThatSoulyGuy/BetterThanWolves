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

public class FCBlockAxlePowerSource extends FCBlockAxle
{
	public FCBlockAxlePowerSource( int iBlockID )
	{
		super( iBlockID );
    	
        setUnlocalizedName( "fcBlockAxlePowerSource" );        

        setCreativeTab( null );
	}
	
	@Override
    public void updateTick( World world, int i, int j, int k, Random random )
    {
		// override of super to prevent power level validation
    }
	
	@Override
    public void onNeighborBlockChange( World world, int i, int j, int k, int iBlockID )
    {
		// override of super to prevent power level validation
    }
	
	@Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return FCBetterThanWolves.fcBlockAxle.blockID;
    }
	
	@Override
    public int GetMechanicalPowerLevelProvidedToAxleAtFacing( World world, int i, int j, int k, int iFacing )
    {
		int iAlignment = GetAxisAlignment( world, i, j, k );
		
		if ( ( iFacing >> 1 ) == iAlignment )
		{
			return 4;
		}
		
    	return 0;
    }
	
	@Override
	public void ValidatePowerLevel( World world, int i, int j, int k )
	{
		// power source axles are validated externally by whatever entity is providing power
	}
	
	@Override
    public int GetPowerLevel( IBlockAccess iBlockAccess, int i, int j, int k )
    {
		return 4;
    }
    
	@Override
    public int GetPowerLevelFromMetadata( int iMetadata )
    {
    	return 4;
    }
    
	@Override
    public void SetPowerLevel( World world, int i, int j, int k, int iPowerLevel )
    {
    }
    
	@Override
    public int SetPowerLevelInMetadata( int iMetadata, int iPowerLevel )
    {
    	return iMetadata;
    }
    
	@Override
    public void SetPowerLevelWithoutNotify( World world, int i, int j, int k, int iPowerLevel )
    {
    }
    
	//----------- Client Side Functionality -----------//
	
	@Override
	public boolean ClientCheckIfPowered( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
}
