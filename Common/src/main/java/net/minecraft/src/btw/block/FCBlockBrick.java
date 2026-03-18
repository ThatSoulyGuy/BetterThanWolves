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

public class FCBlockBrick extends Block
{
    public FCBlockBrick( int iBlockID )
    {
        super( iBlockID, Material.rock );
        
        SetPicksEffectiveOn();
    }
	
    @Override
    public int idDropped( int iMetaData, Random rand, int iFortuneModifier )
    {
        return FCBetterThanWolves.fcBlockBrickLoose.blockID;
    }
    
    @Override
    public void OnBlockDestroyedWithImproperTool( World world, EntityPlayer player, int i, int j, int k, int iMetadata )
    {
        dropBlockAsItem( world, i, j, k, iMetadata, 0 );
    }
    
	@Override
	public boolean HasMortar( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}