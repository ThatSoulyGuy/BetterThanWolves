package net.minecraft.src.btw.item;

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


public class FCItemClub extends Item
{
    private int m_iWeaponDamage;

    public static final int m_iWeaponDamageBone = 4;    
    public static final int m_iDurabilityBone = 20;
    
    public FCItemClub( int iItemID, int iDurability, int iWeaponDamage, String sName )
    {
        super( iItemID );
        
        maxStackSize = 1;
        
        setMaxDamage( iDurability );
        
        m_iWeaponDamage = iWeaponDamage;
        
        SetBuoyant();
        SetIncineratedInCrucible();
        
        setUnlocalizedName( sName );
        
        setCreativeTab( CreativeTabs.tabCombat );
    }

    @Override
    public boolean hitEntity( ItemStack stack, EntityLiving defendingEntity, EntityLiving attackingEntity )
    {
        stack.damageItem( 1, attackingEntity );
        
        return true;
    }

    @Override
    public boolean onBlockDestroyed( ItemStack stack, World world, int iBlockID, int i, int j, int k, EntityLiving usingEntity )
    {
        if ( Block.blocksList[iBlockID].getBlockHardness( world, i, j, k ) > 0F )
        {
            stack.damageItem( 2, usingEntity );
        }

        return true;
    }

    @Override
    public int getDamageVsEntity(Entity par1Entity)
    {
        return m_iWeaponDamage;
    }

    @Override
    public void onCreated( ItemStack stack, World world, EntityPlayer player ) 
    {
		if ( player.m_iTimesCraftedThisTick == 0 && world.isRemote )
		{
			player.playSound( "mob.zombie.woodbreak", 0.1F, 1.25F + ( world.rand.nextFloat() * 0.25F ) );
		}
		
    	super.onCreated( stack, world, player );
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean isFull3D()
    {
        return true;
    }
}
