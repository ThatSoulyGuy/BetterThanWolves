package net.minecraft.src.btw.block;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


public class FCBlockEnchantmentTable extends BlockEnchantmentTable
{
    public FCBlockEnchantmentTable( int iBlockID )
    {
        super( iBlockID );
        
        InitBlockBounds( 0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F );
    }
    
	//------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean shouldSideBeRendered( IBlockAccess blockAccess, 
    	int iNeighborI, int iNeighborJ, int iNeighborK, int iSide )
    {	
    	if ( iSide != 1 )
    	{
    		return super.shouldSideBeRendered( blockAccess, iNeighborI, iNeighborJ, iNeighborK, 
    			iSide );
    	}
    	
    	return true;
    }
}
