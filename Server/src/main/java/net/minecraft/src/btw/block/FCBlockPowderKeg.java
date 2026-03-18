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


import java.util.Random;

public class FCBlockPowderKeg extends BlockTNT
{
	public FCBlockPowderKeg( int iBlockID )
	{
		super( iBlockID );
		
		setHardness( 0F );
		
		SetBuoyant();
		
		SetFireProperties( FCEnumFlammability.EXPLOSIVES );		
		
		setStepSound( soundGrassFootstep );
		
		setUnlocalizedName( "tnt" );
	}
	
	@Override 
    public void onBlockDestroyedByExplosion(World par1World, int par2, int par3, int par4, Explosion par5Explosion)
    {
		// override so that explosion param can be null
        if (!par1World.isRemote)
        {
        	EntityLiving explosionOwner = null;
        	
        	if ( par5Explosion != null )
        	{
        		explosionOwner = par5Explosion.func_94613_c();
        	}
        	
            EntityTNTPrimed var6 = new EntityTNTPrimed(par1World, (double)((float)par2 + 0.5F), (double)((float)par3 + 0.5F), (double)((float)par4 + 0.5F), explosionOwner );
            var6.fuse = par1World.rand.nextInt(var6.fuse / 4) + var6.fuse / 8;
            par1World.spawnEntityInWorld(var6);
        }
    }
    
	@Override 
    public void OnDestroyedByFire( World world, int i, int j, int k, int iFireAge, boolean bForcedFireSpread )
    {
		super.OnDestroyedByFire( world, i, j, k, iFireAge, bForcedFireSpread );
		
        onBlockDestroyedByPlayer( world, i, j, k, 1 );
    }
    
	//----------- Client Side Functionality -----------//
}
