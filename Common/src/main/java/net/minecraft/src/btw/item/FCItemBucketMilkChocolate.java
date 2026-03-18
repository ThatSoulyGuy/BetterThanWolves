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


public class FCItemBucketMilkChocolate extends FCItemBucketDrinkable
{
    public FCItemBucketMilkChocolate( int iItemID )
    {
    	super( iItemID, 9, 0.25F );
    	
        setUnlocalizedName( "fcItemBucketChocolateMilk" );
    }
    
    @Override
    public int getBlockID()
    {
        return FCBetterThanWolves.fcBlockBucketMilkChocolate.blockID;
    }

    @Override
    public ItemStack onEaten( ItemStack itemStack, World world, EntityPlayer player )
    {
        if ( !world.isRemote )
        {
            player.clearActivePotions();
        }
        
        return super.onEaten( itemStack, world, player );
    }
    
    @Override
    public boolean DoesConsumeContainerItemWhenCrafted( Item containerItem )
    {
    	if ( containerItem.itemID == Item.bucketEmpty.itemID )
    	{
    		return true;
    	}
    	
    	return false;
    }
}
