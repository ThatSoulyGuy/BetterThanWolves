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


public class FCBlockBed extends BlockBed
{
    public FCBlockBed( int iBlockID )
    {
    	super( iBlockID );
    	
    	InitBlockBounds( 0D, 0D, 0D, 1D, 0.5625D, 1D );
    }
    
	@Override
    public boolean onBlockActivated( World world, int i, int j, int k, EntityPlayer player, int iFacing, float fXClick, float fYClick, float fZClick )
	{
        if ( world.isRemote )
        {
            player.addChatMessage( "You are too troubled to rest.");            
        }
        
        return true;
	}
    
	@Override
    public void setBlockBoundsBasedOnState( IBlockAccess blockAccess, int i, int j, int k )
    {
    	// override to deprecate parent
    }
	
    //------------- Class Specific Methods ------------//
	
	//----------- Client Side Functionality -----------//
}