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


public class FCBlockLavaFlowing extends BlockFlowing
{
    public FCBlockLavaFlowing( int iBlockID, Material material)
    {
        super( iBlockID, material );
    }
    
    @Override
    public boolean CanPathThroughBlock( IBlockAccess blockAccess, int i, int j, int k, Entity entity, PathFinder pathFinder )
    {
    	return entity.handleLavaMovement();
    }
    
    @Override
    public int GetWeightOnPathBlocked( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return -2;
    }
    
    @Override
    public boolean GetDoesFireDamageToEntities( World world, int i, int j, int k )
    {
    	return true;
    }
    
    @Override
    public boolean GetCanBlockLightItemOnFire( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return true;
    }    
}
