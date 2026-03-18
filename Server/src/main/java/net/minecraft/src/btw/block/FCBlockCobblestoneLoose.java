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


public class FCBlockCobblestoneLoose extends FCBlockLavaReceiver
{
    public FCBlockCobblestoneLoose( int iBlockID )
    {
        super( iBlockID, Material.rock );
        
        setHardness( 1F ); // setHardness( 2F ); regular cobble
        setResistance( 5F ); // setResistance( 10F ); regular cobble
        
        SetPicksEffectiveOn();
        SetChiselsEffectiveOn();
        
        setStepSound( soundStoneFootstep );
        
        setUnlocalizedName( "fcBlockCobblestoneLoose" );        
        
		setCreativeTab( CreativeTabs.tabBlock );
    }
    
    @Override
    public boolean OnMortarApplied( World world, int i, int j, int k )
    {
		world.setBlockWithNotify( i, j, k, Block.cobblestone.blockID );
		
		return true;
    }
    
    //------------- Class Specific Methods ------------//
    
    //------------ Client Side Functionality ----------//
}