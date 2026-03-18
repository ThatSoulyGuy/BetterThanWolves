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


import java.util.List;
import java.util.Random;

public class FCBlockCrucible extends FCBlockCookingVessel
{
    public FCBlockCrucible( int iBlockID )
    {
        super( iBlockID, Material.glass );  
        
        setHardness( 0.6F );
        setResistance( 3F );
        SetPicksEffectiveOn( true );       
        
        setStepSound( soundGlassFootstep );        
        
        setUnlocalizedName( "fcBlockCrucible" );
        
        setCreativeTab( CreativeTabs.tabRedstone );
    }
    
	@Override
    public TileEntity createNewTileEntity( World world )
    {
        return new FCTileEntityCrucible();
    }

    //------------- FCBlockCookingVessel -------------//

	@Override
	public void ValidateFireUnderState( World world, int i, int j, int k )
	{
		// FCTODO: Move this to parent class
		
		if ( !world.isRemote )
		{
			TileEntity tileEnt = world.getBlockTileEntity( i, j, k );
			
			if ( tileEnt instanceof FCTileEntityCrucible )
			{
				FCTileEntityCrucible tileEntityCrucible = 
	            	(FCTileEntityCrucible)tileEnt;
	            
	            tileEntityCrucible.ValidateFireUnderType();            
			}
		}
	}
	
	@Override
	public int GetContainerID()
	{
		return FCBetterThanWolves.fcCrucibleContainerID;
	}
	
    //------------- Class Specific Methods -------------//
    
}