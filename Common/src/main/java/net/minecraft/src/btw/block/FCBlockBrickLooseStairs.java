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


import java.util.Random;

public class FCBlockBrickLooseStairs extends FCBlockMortarReceiverStairs
{
    public FCBlockBrickLooseStairs( int iBlockID )
    {
        super( iBlockID, FCBetterThanWolves.fcBlockBrickLoose, 0 );
        
        SetPicksEffectiveOn();
        
        setUnlocalizedName( "fcBlockBrickLooseStairs" );        
    }
	
    @Override
    public boolean OnMortarApplied( World world, int i, int j, int k )
    {
		world.setBlockAndMetadataWithNotify( i, j, k, Block.stairsBrick.blockID, 
			world.getBlockMetadata( i, j, k ) );
		
		return true;
    }
    
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}