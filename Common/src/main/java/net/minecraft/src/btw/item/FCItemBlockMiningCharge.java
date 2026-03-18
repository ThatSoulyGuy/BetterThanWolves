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


public class FCItemBlockMiningCharge extends ItemBlock
{
    public FCItemBlockMiningCharge( int iItemID )
    {
    	super( iItemID );
    }
    
    @Override
	public boolean OnItemUsedByBlockDispenser( ItemStack stack, World world, 
		int i, int j, int k, int iFacing )
	{
    	FCUtilsBlockPos targetPos = new FCUtilsBlockPos( i, j, k, iFacing );
    	
		FCBlockMiningCharge.CreatePrimedEntity( world, 
			targetPos.i, targetPos.j, targetPos.k, iFacing );
		
        world.playAuxSFX( FCBetterThanWolves.m_iBlockPlaceAuxFXID, i, j, k, 
        	FCBetterThanWolves.fcBlockMiningCharge.blockID );
        
		return true;
	}
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//
}
