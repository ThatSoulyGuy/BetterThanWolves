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

public class FCBlockBucketMilk extends FCBlockBucketFull
{
    public FCBlockBucketMilk( int iBlockID )
    {
        super( iBlockID );
    	
    	setUnlocalizedName( "fcBlockBucketMilk" );
    }
    
	@Override
    public int idDropped( int iMetadata, Random rand, int iFortuneMod )
    {
		return Item.bucketMilk.itemID;
    }
	
	//------------- Class Specific Methods ------------//
	
	@Override
    public boolean AttemptToSpillIntoBlock( World world, int i, int j, int k )
    {
        if ( ( world.isAirBlock( i, j, k ) || !world.getBlockMaterial( i, j, k ).isSolid() ) )
        {     
    		world.setBlockWithNotify( i, j, k, FCBetterThanWolves.fcBlockMilk.blockID );
            
            return true;
        }
        
        return false;
    }
	
	//----------- Client Side Functionality -----------//
	
    private Icon m_iconContents;
    
	@Override
    public void registerIcons( IconRegister register )
    {
		super.registerIcons( register );
		
		m_iconContents = register.registerIcon( "fcBlockBucket_milk" );
    }
	
	@Override
	public Icon GetContentsIcon()
	{
		return m_iconContents;
	}
}
