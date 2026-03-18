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

public class FCBlockMilk extends FCBlockFalling
{
	public static final double m_dHeight = ( 1D / 16D );

	public static final int m_iDecayTickRate = 10;
	
    public FCBlockMilk( int iBlockID )
    {
        super( iBlockID, FCBetterThanWolves.fcMaterialMilk );
        
        InitBlockBounds( 0D, 0D, 0D, 1D, m_dHeight, 1D );
        
        setHardness( 0F );
        setResistance( 0F );
        
        setStepSound( FCBetterThanWolves.fcStepSoundSquish );
        
        setUnlocalizedName( "fcBlockMilk" );
    }
    
    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return 0;
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
    public void updateTick( World world, int i, int j, int k, Random rand ) 
    {
    	if ( !CheckForFall( world, i, j, k ) )
    	{    	
    		int iDecayLevel = GetDecayLevel( world, i, j, k );
    		
    		if ( iDecayLevel < 1 )
    		{
    			iDecayLevel++;
    			
    			SetDecayLevel( world, i, j, k, iDecayLevel );
    			
    	        world.scheduleBlockUpdate( i, j, k, blockID, m_iDecayTickRate );
    		}
    		else
    		{
    			world.setBlockToAir( i, j, k );
    		}
    	}
    }
    
	@Override
    public void onStartFalling( EntityFallingSand entity )
    {
		entity.metadata = SetDecayLevel( entity.metadata, 0 );
    }
	
	//------------- Class Specific Methods ------------//
	
	public int GetDecayLevel( IBlockAccess blockAccess, int i, int j, int k )
	{
		return GetDecayLevel( blockAccess.getBlockMetadata( i, j, k ) );
	}
	
	public int GetDecayLevel( int iMetadata )
	{
		return iMetadata & 1; 
	}
	
	public void SetDecayLevel( World world, int i, int j, int k, int iLevel )
	{
		int iMetadata = SetDecayLevel( world.getBlockMetadata( i, j, k ), iLevel );
		
		world.setBlockMetadataWithNotify( i, j, k, iMetadata );
	}
	
	public int SetDecayLevel( int iMetadata, int iLevel )
	{
		iMetadata &= ~1;
		
		return iMetadata | iLevel;
	}
    
	//----------- Client Side Functionality -----------//
}
