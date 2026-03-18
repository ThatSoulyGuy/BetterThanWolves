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


import java.util.Random;

public class FCBlockTorchLegacy extends FCBlockTorchBaseBurning
{
    public FCBlockTorchLegacy( int iBlockID )
    {
    	super( iBlockID );
    	
    	setLightValue( 0.9375F );
    	
    	setUnlocalizedName( "torch" );
    }

    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return FCBetterThanWolves.fcBlockTorchNetherBurning.blockID;
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public void randomDisplayTick( World world, int i, int j, int k, Random rand )
    {
    	// legacy torches don't display flame particles to help spot them
    	
    	Vec3 pos = GetParticalPos( world, i, j, k );
    	
        world.spawnParticle( "smoke", pos.xCoord, pos.yCoord, pos.zCoord, 0D, 0D, 0D );
    }    
}
