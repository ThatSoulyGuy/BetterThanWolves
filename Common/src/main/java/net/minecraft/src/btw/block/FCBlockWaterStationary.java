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


public class FCBlockWaterStationary extends BlockStationary
{
    public FCBlockWaterStationary( int iBlockID, Material material )
    {
        super( iBlockID, material );
    }
    
    @Override
    public boolean CanPathThroughBlock( IBlockAccess blockAccess, int i, int j, int k, Entity entity, PathFinder pathFinder )
    {
    	return pathFinder.CanPathThroughWater();
    }
    
    @Override
    public int GetWeightOnPathBlocked( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return -1;
    }

    @Override
    public int AdjustPathWeightOnNotBlocked( int iPreviousWeight )
    {
    	return 2;
    }
}  
