package net.minecraft.src.btw.item;

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


public class FCItemBoat extends ItemBoat
{
    public FCItemBoat( int iItemID )
    {
    	super( iItemID );

    	SetBuoyant();
    	SetIncineratedInCrucible();
    	
    	// same as 5 jungle sidings to be equivalent to crappiest recipe
    	SetFurnaceBurnTime( 5 * FCEnumFurnaceBurnTime.PLANKS_JUNGLE.m_iBurnTime / 2 );
    	
    	setUnlocalizedName( "boat" );
    }
    
    @Override
	public boolean OnItemUsedByBlockDispenser( ItemStack stack, World world, 
		int i, int j, int k, int iFacing )
	{
        FCUtilsBlockPos offsetPos = new FCUtilsBlockPos( 0, 0, 0, iFacing );
        
        double dXPos = i + ( offsetPos.i * 1.6D ) + 0.5D;
        double dYPos = j + offsetPos.j;
        double dZPos = k + ( offsetPos.k * 1.6D ) + 0.5D;
    	
    	double dBoatYPos = j + offsetPos.j;                	
    	
        Entity entity = new EntityBoat( world, dXPos, dYPos, dZPos );
        
        world.spawnEntityInWorld( entity );
        
        world.playAuxSFX( 1000, i, j, k, 0 ); // normal pitch click							        
        
		return true;
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
