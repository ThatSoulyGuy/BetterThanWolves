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


public class FCItemCanvas extends Item
{
    public FCItemCanvas( int iItemID )
    {
        super( iItemID );
        
        SetBuoyant();
        
    	setUnlocalizedName( "fcItemCanvas" );
    	
    	setCreativeTab( CreativeTabs.tabDecorations );
    }

    @Override
    public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )    
    {
    	// can't be placed upwards or downwards 
    	
        if ( iFacing == 0 || iFacing == 1 )
        {
            return false;
        }

        if ( !player.canPlayerEdit( i, j, k, iFacing, itemStack ) )
        {
            return false;
        }

        byte entityFacing = 0;

        if ( iFacing == 4 )
        {
        	entityFacing = 1;
        }
        else if ( iFacing == 3 )
        {
        	entityFacing = 2;
        }
        else if ( iFacing == 5 )
        {
        	entityFacing = 3;
        }

        FCEntityCanvas canvas = new FCEntityCanvas( world, i, j, k, entityFacing );

        if ( canvas.onValidSurface() )
        {
            if ( !world.isRemote )
            {
                world.spawnEntityInWorld( canvas );
            }

            itemStack.stackSize--;
        }

        return true;
    }
}
