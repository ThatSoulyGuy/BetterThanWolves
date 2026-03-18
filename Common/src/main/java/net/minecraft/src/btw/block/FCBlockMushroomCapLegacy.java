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


public class FCBlockMushroomCapLegacy extends FCBlockMushroomCap
{
    public FCBlockMushroomCapLegacy( int iBlockID, int iMushroomType )
    {
        super( iBlockID, iMushroomType );
        
		SetFireProperties( FCEnumFlammability.NONE );
    }
    
	@Override
    public void onBlockAdded( World world, int i, int j, int k )
    {
        super.onBlockAdded( world, i, j, k );

        // convert vanilla blocks to new flammable ones on add so that we don't have to modify
        // WorldGenBigMushroom

        int iNewBlockID = FCBetterThanWolves.fcBlockMushroomCapBrown.blockID;
        
        if ( m_iMushroomType != 0 )
        {
        	iNewBlockID = FCBetterThanWolves.fcBlockMushroomCapRed.blockID;
        }
        
    	// "2" in last param to not trigger another neighbor block notify
    	
        world.setBlock( i, j, k, iNewBlockID, world.getBlockMetadata( i, j, k ), 2 );
    }
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
