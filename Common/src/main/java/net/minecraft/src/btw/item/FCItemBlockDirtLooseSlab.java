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


public class FCItemBlockDirtLooseSlab extends FCItemBlockSlab
{
    public FCItemBlockDirtLooseSlab( int iItemID )
    {
        super( iItemID );        
    }
    
    @Override
    public boolean canCombineWithBlock( World world, int i, int j, int k, int iItemDamage )
    {
        int iBlockID = world.getBlockId( i, j, k );
        
        if ( iBlockID == FCBetterThanWolves.fcBlockDirtSlab.blockID )
        {
            int iMetadata = world.getBlockMetadata( i, j, k );
            int iSubtype = FCBetterThanWolves.fcBlockDirtSlab.GetSubtype( iMetadata );
            
            if ( iSubtype != FCBlockDirtSlab.m_iSubtypePackedEarth )
            {
            	if ( !FCBetterThanWolves.fcBlockDirtSlab.GetIsUpsideDown( iMetadata ) )
            	{
            		return true;
            	}
            }
            
            return false;
        }
        else if ( iBlockID == FCBetterThanWolves.fcBlockMyceliumSlab.blockID )
        {
    		return true;
        }
        
    	return super.canCombineWithBlock( world, i, j, k, iItemDamage );
    }    

    @Override
    public boolean convertToFullBlock( World world, int i, int j, int k )
    {
    	// force target to convert to loose dirt, to handle stuff like combining with grass slabs
    	
    	int iNewBlockID = ((FCBlockSlab)Block.blocksList[getBlockID()]).GetCombinedBlockID( 0 );
    	
    	return world.setBlockWithNotify( i, j, k, iNewBlockID );
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
} 
