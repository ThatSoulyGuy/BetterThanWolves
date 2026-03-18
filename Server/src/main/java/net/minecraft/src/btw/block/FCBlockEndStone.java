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


public class FCBlockEndStone extends FCBlockFullBlock
{
    public FCBlockEndStone( int iBlockID, Material material )
    {
    	super( iBlockID, material );
    	
		SetCanBeCookedByKiln( true );
    }

    @Override
    public void OnCookedByKiln( World world, int i, int j, int k )
    {
    	world.setBlockWithNotify( i, j, k, 0 );

		FCUtilsItem.EjectSingleItemWithRandomOffset( world, i, j, k, FCBetterThanWolves.fcItemEnderSlag.itemID, 0 );
		
		FCUtilsItem.EjectSingleItemWithRandomOffset( world, i, j, k, FCBetterThanWolves.fcAestheticOpaque.blockID, FCBlockAestheticOpaque.m_iSubtypeWhiteCobble );
    }
    
    @Override
    public float GetMovementModifier( World world, int i, int j, int k )
    {
    	return 1.0F;
    }
    
    @Override
    public int GetCookTimeMultiplierInKiln( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return 8;
    }
    
	//----------- Client Side Functionality -----------//
}
