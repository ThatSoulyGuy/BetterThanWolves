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


public class FCBlockGlass extends BlockGlass
{
    public FCBlockGlass( int iBlockID, Material material, boolean bRenderFacesNeighboringOnSameBlock )
    {
        super( iBlockID, material, bRenderFacesNeighboringOnSameBlock );
        
        SetPicksEffectiveOn( true );        
    }
    
    @Override
	public boolean HasLargeCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
	{
		return bIgnoreTransparency;
	}
    
    @Override
	public boolean CanRotateOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}	
	
    @Override
	public boolean CanTransmitRotationHorizontallyOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
	
    @Override
	public boolean CanTransmitRotationVerticallyOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		return true;
	}
}
