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


public class FCBlockChunkOreStorageIron extends FCBlockChunkOreStorage
{
    public FCBlockChunkOreStorageIron( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName( "fcBlockChunkOreStorageIron" );
    }
    
	@Override
	public boolean DropComponentItemsOnBadBreak( World world, int i, int j, int k, int iMetadata, float fChanceOfDrop )
	{
		DropItemsIndividualy( world, i, j, k, FCBetterThanWolves.fcItemChunkIronOre.itemID, 
			9, 0, fChanceOfDrop );
		
		return true;
	}	
	
	@Override
    public int GetItemIndexDroppedWhenCookedByKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
		return Item.ingotIron.itemID;
    }
    
	//------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}