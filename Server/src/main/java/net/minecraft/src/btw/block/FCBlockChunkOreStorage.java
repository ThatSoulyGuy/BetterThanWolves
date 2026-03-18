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


public abstract class FCBlockChunkOreStorage extends FCBlockFallingFullBlock
{
    public FCBlockChunkOreStorage( int iBlockID )
    {
        super( iBlockID, Material.rock );
        
        setHardness( 1F );
        setResistance( 5F );
        SetPicksEffectiveOn();
        
        setStepSound( soundStoneFootstep );
        
		setCreativeTab( CreativeTabs.tabBlock );
        
		SetCanBeCookedByKiln( true );
    }
    
    @Override
    public int GetCookTimeMultiplierInKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 8;
    }
    
	//------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
