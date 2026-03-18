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


public class FCItemBlockTorchFiniteIdle extends FCItemBlockTorchIdle
{	
    public FCItemBlockTorchFiniteIdle( int iItemID )
    {
        super( iItemID );
        
        setUnlocalizedName( "fcBlockTorchFiniteIdle" );
    }
    
    @Override
    public ItemStack OnRightClickOnIgniter( ItemStack stack, World world, EntityPlayer player )
    {
        int i = MathHelper.floor_double(player.posX);
        int j = MathHelper.floor_double(player.boundingBox.minY);
        int k = MathHelper.floor_double(player.posZ);
        
        player.playSound( "mob.ghast.fireball", 1.0F, world.rand.nextFloat() * 0.4F + 0.8F );
        
        ItemStack newStack = new ItemStack( FCBetterThanWolves.fcBlockTorchFiniteBurning, 1, 0 );
        
        long iExpiryTime = FCUtilsWorld.GetOverworldTimeServerOnly() + (long)FCTileEntityTorchFinite.m_iMaxBurnTime;
        
        newStack.setTagCompound( new NBTTagCompound() );
        newStack.getTagCompound().setLong( "outTime", iExpiryTime);

		stack.stackSize--;
		
        if ( stack.stackSize <= 0 )
        {
        	return newStack; 
        }
        
		FCUtilsItem.GivePlayerStackOrEject( player, newStack, i, j, k );
		
		return stack;
    }    

	//------------- Class Specific Methods ------------//
}
