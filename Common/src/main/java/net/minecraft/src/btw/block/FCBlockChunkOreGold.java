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

public class FCBlockChunkOreGold extends FCBlockChunkOre
{
    public FCBlockChunkOreGold( int iBlockID )
    {
        super( iBlockID );
        
        setUnlocalizedName( "fcBlockChunkOreGold" );
    }
    
	@Override
    public int idDropped( int iMetadata, Random random, int iFortuneModifier )
    {
		return FCBetterThanWolves.fcItemChunkGoldOre.itemID;
    }
	
	@Override
    public int GetItemIndexDroppedWhenCookedByKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
		return Item.goldNugget.itemID;
    }

}