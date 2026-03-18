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

public class FCBlockCauldron extends FCBlockCookingVessel
{
    public FCBlockCauldron( int iBlockID )
    {
        super( iBlockID, Material.iron );
        
        setHardness( 3.5F );
        setResistance( 10F );
        
        setStepSound( soundMetalFootstep );
        
        setUnlocalizedName( "fcBlockCauldron" );        
        
        setCreativeTab( CreativeTabs.tabRedstone );
    }

	@Override
    public TileEntity createNewTileEntity( World world )
    {
        return new FCTileEntityCauldron();
    }
    
    //------------- FCBlockCookingVessel -------------//

	@Override
	public void ValidateFireUnderState( World world, int i, int j, int k )
	{
		// FCTODO: Move this to parent class
		
		if ( !world.isRemote )
		{
			TileEntity tileEnt = world.getBlockTileEntity( i, j, k );
			
			if ( tileEnt instanceof FCTileEntityCauldron )
			{
				FCTileEntityCauldron tileEntityCauldron = 
	            	(FCTileEntityCauldron)tileEnt;
	            
	            tileEntityCauldron.ValidateFireUnderType();            
			}
		}
	}
	
	@Override
	public int GetContainerID()
	{
		return FCBetterThanWolves.fcCauldronContainerID;
	}
	
    //------------- Class Specific Methods -------------//
    
}