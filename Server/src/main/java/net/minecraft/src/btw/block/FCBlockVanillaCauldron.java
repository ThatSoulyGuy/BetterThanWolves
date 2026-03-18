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


import java.util.List;

public class FCBlockVanillaCauldron extends BlockCauldron
{
    public FCBlockVanillaCauldron( int iBlockID )
    {
        super( iBlockID );
        
        InitBlockBounds( 0F, 0F, 0F, 1F, 1F, 1F );
    }
    
    @Override
    public void addCollisionBoxesToList( World world, int i, int j, int k, 
    	AxisAlignedBB intersectingBox, List list, Entity entity )
    {
    	// parent method is super complicated for no apparent reason
    	
        AxisAlignedBB tempBox = getCollisionBoundingBoxFromPool( world, i, j, k );
    	
    	tempBox.AddToListIfIntersects( intersectingBox, list );    	
    }
    
	//------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}
