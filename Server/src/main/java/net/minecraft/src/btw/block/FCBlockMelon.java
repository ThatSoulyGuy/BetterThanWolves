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


public class FCBlockMelon extends FCBlockGourd
{
    public FCBlockMelon( int iBlockID )
    {
        super( iBlockID );        
    }

    @Override
    public int GetItemIDDroppedOnSaw( World world, int i, int j, int k )
    {
    	return Item.melon.itemID;
    }
    
    @Override
    public int GetItemCountDroppedOnSaw( World world, int i, int j, int k )
    {
    	return 5;
    }
    
    //------------- FCBlockGourd ------------//
    
    @Override
	public Item ItemToDropOnExplode()
    {
    	return Item.melonSeeds;
    }
	
    @Override
	public int ItemCountToDropOnExplode()
    {
    	return 5;
    }
	
    @Override
	public int AuxFXIDOnExplode()
    {
    	return FCBetterThanWolves.m_iMelonExplodeAuxFXID;
    }
    
	public DamageSource GetFallDamageSource()
	{
		return FCDamageSourceCustom.m_DamageSourceMelon;
	}
	
}
