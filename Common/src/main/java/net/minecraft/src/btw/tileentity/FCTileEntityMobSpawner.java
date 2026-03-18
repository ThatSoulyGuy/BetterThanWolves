package net.minecraft.src.btw.tileentity;

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


import java.util.Iterator;

public class FCTileEntityMobSpawner extends TileEntityMobSpawner
{
    public FCTileEntityMobSpawner()
    {
        super();
    }

    @Override
    public void updateEntity()
    {    	
        if ( !worldObj.isRemote )
        {
			// check for generation of mossy cobble
			
	    	if ( worldObj.rand.nextInt( 1200 ) == 0 ) // about once a minute
	    	{
	            if ( worldObj.checkChunksExist( xCoord - 4, yCoord - 1, zCoord - 4, xCoord + 4, yCoord + 4, zCoord + 4 ) )
	            {
		    		// generate a random co-ordinate around the spawner
		    		
		    		int iOffset = worldObj.rand.nextInt( 9 );
		    		int jOffset = worldObj.rand.nextInt( 6 );
		    		int kOffset = worldObj.rand.nextInt( 9 );
		    		
		    		int targetI = xCoord - 4 + iOffset;
		    		int targetJ = yCoord - 1 + jOffset;
		    		int targetK = zCoord - 4 + kOffset;
		    		
		    		if ( worldObj.getBlockId( targetI, targetJ, targetK ) == Block.cobblestone.blockID )
		    		{
		    			worldObj.setBlockWithNotify( targetI, targetJ, targetK, Block.cobblestoneMossy.blockID );
		    		}
	            }
	    	}
        }
        
    	super.updateEntity();
    }    
}