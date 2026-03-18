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

public class FCBlockNetherrack extends FCBlockFullBlock
{
    public FCBlockNetherrack( int iBlockID )
    {
        super( iBlockID, FCBetterThanWolves.fcMaterialNetherRock );
        
        setHardness( 0.6F );        
        setResistance( 0.4F * 5F / 3F ); // 0.4 was original hardness of netherrack.  Equation to preserve its original blast resistance.
        SetPicksEffectiveOn();
        
        setStepSound( soundStoneFootstep );
        
        setCreativeTab( CreativeTabs.tabBlock );
        
        setUnlocalizedName( "hellrock" );        
    }
    
    @Override
    public float GetMovementModifier( World world, int i, int j, int k )
    {
    	return 1F;
    }    
    
    @Override
    public int GetEfficientToolLevel( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 2; // iron and better
    }
    
	@Override
    public void onBlockAdded( World world, int i, int j, int k )
    {
        super.onBlockAdded( world, i, j, k );
        
        if ( !world.provider.isHellWorld )
        {
        	// "2" in last param to not trigger another neighbor block notify
        	
            world.setBlock( i, j, k, FCBetterThanWolves.fcBlockNetherrackFalling.blockID, 0, 2 );
        }
    }
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
