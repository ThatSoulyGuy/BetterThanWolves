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

//FCMOD


public class FCItemNetherStar extends ItemSimpleFoiled
{
    public FCItemNetherStar( int iItemID )
    {
        super( iItemID );

		SetFilterableProperties( m_iFilterable_Small );
		
        setUnlocalizedName( "netherStar" );
        
        setCreativeTab( CreativeTabs.tabMaterials );    
    }
    
    @Override
    public void OnUsedInCrafting( EntityPlayer player, ItemStack outputStack )
    {
		// note: the playSound function for player both plays the sound locally on the client, and plays it remotely on the server without it being sent again to the same player
    	
		if ( player.m_iTimesCraftedThisTick == 0 )
		{
			player.playSound( "ambient.cave.cave4", 0.5F, player.worldObj.rand.nextFloat() * 0.05F + 0.5F );
		}
    }
}
