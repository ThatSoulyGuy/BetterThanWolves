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

public class FCBlockLightBulb extends Block
{
    private final static int iLightBulbTickRate = 2;
    
    private boolean m_bGlowing; 
    
    public FCBlockLightBulb( int iBlockID, boolean bGlowing )
    {
    	super( iBlockID, Material.glass );

        setHardness( 0.4F );
        SetPicksEffectiveOn();        
        
        setStepSound( Block.soundGlassFootstep );
        
        setUnlocalizedName( "fcBlockLightBlock" );
        
        m_bGlowing = bGlowing;
        
        if ( bGlowing )
        {
        	setLightValue( 1F );
        }
        else
        {
            setCreativeTab( CreativeTabs.tabRedstone );
        }
        
        setTickRandomly( true );
    }
    
	@Override
    public int tickRate( World world )
    {
        return iLightBulbTickRate;
    }    

	@Override
    public void onBlockAdded( World world, int i, int j, int k )
    {
    	world.scheduleBlockUpdate( i, j, k, blockID, tickRate( world ) );
    }
    
	@Override
    public int idDropped( int iMetaData, Random random, int iFortuneModifier )
    {
    	// override the dropped id so we won't get dropped lit lightbulbs
    	
        return FCBetterThanWolves.fcLightBulbOff.blockID;
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
    public void updateTick( World world, int i, int j, int k, Random random )
    {
        boolean bPowered = world.isBlockIndirectlyGettingPowered(i, j, k);

        if ( bPowered )
        {
        	if ( !IsLightOn( world, i, j, k ) )
        	{
        		LightBulbTurnOn( world, i, j, k );
        		
        		return;
        	}
        }
    	else
    	{
    		if ( IsLightOn( world, i, j, k ) )
    		{
    			LightBulbTurnOff( world, i, j, k );
    			
        		return;
    		}    		
    	}
    }
    
	@Override
    public void RandomUpdateTick( World world, int i, int j, int k, Random rand )
    {
		if ( !IsCurrentStateValid( world, i, j, k ) )
		{
			// verify we have a tick already scheduled to prevent jams on chunk load
			
			if ( !world.IsUpdateScheduledForBlock( i, j, k, blockID ) )
			{
		        world.scheduleBlockUpdate( i, j, k, blockID, tickRate( world ) );        
			}
		}
    }
	
	public boolean IsCurrentStateValid( World world, int i, int j, int k )
	{
        boolean bPowered = world.isBlockIndirectlyGettingPowered( i, j, k );

        return bPowered == IsLightOn( world, i, j, k );
	}
	
	@Override
    public void onNeighborBlockChange(World world, int i, int j, int k, int l)
    {
		if ( !IsCurrentStateValid( world, i, j, k ) &&
			!world.IsUpdatePendingThisTickForBlock( i, j, k, blockID ) )
		{
			world.scheduleBlockUpdate( i, j, k, blockID, tickRate( world ) );
		}
    }
	
	@Override
	public boolean CanRotateOnTurntable( IBlockAccess iBlockAccess, int i, int j, int k )
	{
		return true;
	}
	
	@Override
	public boolean CanTransmitRotationHorizontallyOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
	
	@Override
	public boolean CanTransmitRotationVerticallyOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
	
	@Override
	public boolean HasLargeCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
	{
		return bIgnoreTransparency;
	}
	
	//------------- Class Specific Methods ------------//
    
    private void LightBulbTurnOn( World world, int i, int j, int k )
    {
        world.setBlockWithNotify( i, j, k, FCBetterThanWolves.fcLightBulbOn.blockID );        
    }
    
    private void LightBulbTurnOff( World world, int i, int j, int k )
    {
        world.setBlockWithNotify( i, j, k, FCBetterThanWolves.fcLightBulbOff.blockID );
   }
    
    public boolean IsLightOn(World world, int i, int j, int k )
    {
    	return world.getBlockId(i, j, k) == FCBetterThanWolves.fcLightBulbOn.blockID;    	
    }
    
	//----------- Client Side Functionality -----------//
}
