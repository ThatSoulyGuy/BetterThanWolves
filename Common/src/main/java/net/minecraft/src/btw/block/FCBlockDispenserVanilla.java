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


public class FCBlockDispenserVanilla extends BlockDispenser
{
    public FCBlockDispenserVanilla( int iBlockID )
    {
    	super( iBlockID );
    	
    	setHardness( 3.5F );
    	
    	setStepSound( soundStoneFootstep );
    	
    	setUnlocalizedName( "dispenser" );
    }

    @Override
	public int GetFacing( int iMetadata )
	{
		return iMetadata & 7;
	}
	
    @Override
	public int SetFacing( int iMetadata, int iFacing )
	{
		return ( iMetadata & (~7) ) | iFacing;
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}

