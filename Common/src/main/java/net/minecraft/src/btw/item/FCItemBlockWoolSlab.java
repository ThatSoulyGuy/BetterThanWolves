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


public class FCItemBlockWoolSlab extends FCItemBlockSlab
{
    public FCItemBlockWoolSlab( int i )
    {
        super( i );
        
        setHasSubtypes( true );
        
        setUnlocalizedName( "fcBlockWoolSlab" );
    }

    @Override
    public int getMetadata( int i )
    {
    	return i;
    }
    
    @Override
    public int GetBlockIDToPlace( int iItemDamage, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
        if ( iFacing == 0 || iFacing != 1 && (double)fClickY > 0.5D )
        {
			return FCBetterThanWolves.fcBlockWoolSlabTop.blockID;
        }
        
		return FCBetterThanWolves.fcWoolSlab.blockID;
    }
    
    @Override
    public boolean canCombineWithBlock( World world, int i, int j, int k, int iItemDamage )
    {
        int iBlockID = world.getBlockId( i, j, k );
        int iBlockMetadata = world.getBlockMetadata( i, j, k );
        
        if ( ( iBlockID == FCBetterThanWolves.fcWoolSlab.blockID || iBlockID == FCBetterThanWolves.fcBlockWoolSlabTop.blockID ) && 
    		iBlockMetadata == iItemDamage )
        {
        	return true;
        }
        
    	return false;
    }
    
    //------------- Class Specific Methods ------------//
}
