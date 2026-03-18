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

public class FCBlockTorchBaseBurning extends FCBlockTorchBase
{
    public FCBlockTorchBaseBurning( int iBlockID )
    {
    	super( iBlockID );
    }
    
    @Override
    public boolean GetCanBlockLightItemOnFire( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return true;
    }    
    
    @Override
	public void OnFluidFlowIntoBlock( World world, int i, int j, int k, BlockFluid newBlock )
	{
    	if ( newBlock.blockMaterial == Material.water )
    	{
	        world.playAuxSFX( FCBetterThanWolves.m_iFireFizzSoundAuxFXID, i, j, k, 0 );
	        
	        dropBlockAsItem_do( world, i, j, k, new ItemStack( FCBetterThanWolves.fcBlockTorchNetherUnlit.blockID, 1, 0 ) );
    	}
    	else
    	{
    		super.OnFluidFlowIntoBlock( world, i, j, k, newBlock );
    	}
	}

	//----------- Client Side Functionality -----------//
}
