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

public class FCBlockCarrot extends BlockCarrot
{
    public FCBlockCarrot( int iBlockID )
    {
        super( iBlockID );
    }
    
    @Override
    public float GetBaseGrowthChance( World world, int i, int j, int k )
    {
    	return 0.1F;
    }
    
    @Override
    public void IncrementGrowthLevel( World world, int i, int j, int k )
    {
    	int iGrowthLevel = GetGrowthLevel( world, i, j, k ) + 1;

    	if ( iGrowthLevel == 7 || ( iGrowthLevel & 1 ) == 0 )
    	{
    		super.IncrementGrowthLevel( world, i, j, k );
    	}
    	else
    	{    	
    		// only notify surrounding blocks and the client on visible change
    		
            SetGrowthLevelNoNotify( world, i, j, k, iGrowthLevel );
    	}    
	}    
	
    @Override
    public ItemStack GetStackRetrievedByBlockDispenser( World world, int i, int j, int k )
    {
		// only retrieve if fully grown
		
		if ( world.getBlockMetadata( i, j, k ) >= 7 )
		{
			return super.GetStackRetrievedByBlockDispenser( world, i, j, k );			
		}
    	
    	return null;
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
