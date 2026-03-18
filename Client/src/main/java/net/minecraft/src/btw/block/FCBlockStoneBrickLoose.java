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


public class FCBlockStoneBrickLoose extends FCBlockLavaReceiver
{
    public FCBlockStoneBrickLoose( int iBlockID )
    {
        super( iBlockID, Material.rock );
        
        setHardness( 1F ); // setHardness( 2.25F ); regular stone brick
        setResistance( 5F ); // setResistance( 10F ); regular stone brick
        
        SetPicksEffectiveOn();
        
        setStepSound( soundStoneFootstep );
        
        setUnlocalizedName( "fcBlockStoneBrickLoose" );        
        
		setCreativeTab( CreativeTabs.tabBlock );
    }
    
    @Override
    public boolean OnMortarApplied( World world, int i, int j, int k )
    {
		world.setBlockWithNotify( i, j, k, Block.stoneBrick.blockID );
		
		return true;
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//    
    
    private Icon m_iconLavaCracks;
    
    @Override
    public void registerIcons( IconRegister register )
    {
        super.registerIcons( register );
        
        m_iconLavaCracks = register.registerIcon( "fcOverlayStoneBrickLava" );
    }
    
    @Override
    public Icon GetLavaCracksOverlay()
    {
    	return m_iconLavaCracks;
    }    
}