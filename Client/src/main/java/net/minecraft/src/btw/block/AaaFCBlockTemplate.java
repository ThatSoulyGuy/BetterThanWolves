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


public class AaaFCBlockTemplate extends Block
{
    public AaaFCBlockTemplate( int iBlockID, Material material )
    {
    	super( iBlockID, Material.rock );
    	
        setHardness( 1F );
        setResistance( 10F ); // most blocks don't need setResistance() as it's done in setHardness()
        SetShovelsEffectiveOn( false );
        SetPicksEffectiveOn( false );
        SetAxesEffectiveOn( false );
        SetChiselsEffectiveOn( false );
        
        InitBlockBounds( 0F, 0F, 0F, 1F, 1F, 1F );
        
        SetNonBuoyant();
		SetFireProperties( FCEnumFlammability.NONE );
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.NONE );
    	SetFilterableProperties( Item.m_iFilterable_SolidBlock );
		SetCanBeCookedByKiln( false );
        
        setLightOpacity( 255 ); // most don't need. 255 is fully opaque
        Block.useNeighborBrightness[iBlockID] = false; // used by slabs and such
        
        setStepSound( soundStoneFootstep );        
        
        setUnlocalizedName( "fcBlockTemplate" );
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
