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

public class FCBlockDaylightDetector extends BlockDaylightDetector
{
    public FCBlockDaylightDetector(int par1)
    {
        super( par1 );
		
        setCreativeTab( null );
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        return 0;
    }

    @Override
    public void updateLightLevel(World par1World, int par2, int par3, int par4)
    {
    }

    @Override
    public boolean canProvidePower()
    {
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World par1World)
    {
        return null;
    }

}
