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

public class FCBlockTorchNetherBurning extends FCBlockTorchBaseBurning
{
    public FCBlockTorchNetherBurning( int iBlockID )
    {
    	super( iBlockID );
    	
    	setLightValue( 0.9375F );
    	
    	setUnlocalizedName( "fcBlockTorchNetherBurning" );
    	
    	setTickRandomly( true );
    }
	
    @Override
    public void updateTick( World world, int i, int j, int k, Random rand )
    {
        super.updateTick( world, i, j, k, rand );
        
        // last param provides increased chance of fire spread, over default of 100
        
		FCBlockFire.CheckForFireSpreadAndDestructionToOneBlockLocation( world, i, j + 1, k, rand, 0, 25 );
    }
    
	@Override
    public boolean CanBeCrushedByFallingEntity( World world, int i, int j, int k, EntityFallingSand entity )
    {
    	return true;
    }
    
    //------------- Class Specific Methods ------------//

	//----------- Client Side Functionality -----------//
}
