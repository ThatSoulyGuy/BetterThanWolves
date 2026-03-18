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


public class FCBlockMobSpawner extends BlockMobSpawner
{
    public FCBlockMobSpawner( int iBlockID )
    {
        super( iBlockID );
        
        setHardness( 5F );
        
        setStepSound( soundMetalFootstep );
        
        setUnlocalizedName( "mobSpawner" );
        
        disableStats();
    }
    
    @Override
    public TileEntity createNewTileEntity( World world )
    {
        return new FCTileEntityMobSpawner();
    }
    
    @Override
    public ItemStack GetStackRetrievedByBlockDispenser( World world, int i, int j, int k )
    {	 
    	return null; // can't be picked up
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
