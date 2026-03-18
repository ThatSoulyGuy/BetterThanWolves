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

public class FCBlockOreIron extends FCBlockOreStaged
{
    public FCBlockOreIron( int iBlockID )
    {
        super( iBlockID );
        
		SetCanBeCookedByKiln( true );
    }
    
    @Override
    public int idDropped( int iMetadata, Random rand, int iFortuneModifier )
    {
        return FCBetterThanWolves.fcItemChunkIronOre.itemID;
    }
    
    @Override
    public int IdDroppedOnConversion( int iMetadata )
    {
        return FCBetterThanWolves.fcItemPileIronOre.itemID;
    }
    
    @Override
    public int GetRequiredToolLevelForOre( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 1;
    }
    
    @Override
    public int GetItemIndexDroppedWhenCookedByKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// have to do this here instead of through SetItemIndexDroppedWhenCookedByKiln() 
    	// because of vanilla instatiating this block before the corresponding item
    	
    	return FCBetterThanWolves.fcItemNuggetIron.itemID;
    }
    
    @Override
    public int GetCookTimeMultiplierInKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 8;
    }
    
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
