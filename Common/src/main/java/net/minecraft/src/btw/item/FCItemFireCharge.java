package net.minecraft.src.btw.item;

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


public class FCItemFireCharge extends ItemFireball
{
    public FCItemFireCharge( int iItemID )
    {
        super( iItemID );
        
        setUnlocalizedName( "fireball" );
    }

    @Override
    public boolean onItemUse( ItemStack stack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )
    {
        if ( !world.isRemote )
        {
        	Block clickedBlock = Block.blocksList[world.getBlockId( i, j, k )];
        	
        	if ( clickedBlock != null && clickedBlock.GetCanBeSetOnFireDirectlyByItem( world, i, j, k ) )
        	{
        		clickedBlock.SetOnFireDirectly( world, i, j, k );
        		
                if ( !player.capabilities.isCreativeMode )
                {
                	stack.stackSize--;
                }
                
        		return true;
        	}
        }
        
		return super.onItemUse( stack, player, world, i, j, k, iFacing, fClickX, fClickY, fClickZ );
    }
    
    //------------- Class Specific Methods ------------//
    
	//------------ Client Side Functionality ----------//    
}
