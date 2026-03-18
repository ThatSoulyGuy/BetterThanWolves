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


public class FCItemChiselIron extends FCItemChisel
{	
    public FCItemChiselIron( int iItemID )
    {
    	// the number of uses has to be less than roughly 15% of pick to prevent it being more 
    	// efficient when used to harvest diamonds
    	
        super( iItemID, EnumToolMaterial.IRON, 50 );
        
        efficiencyOnProperMaterial /= 6;
        
        SetFilterableProperties( Item.m_iFilterable_Narrow );
        
        setUnlocalizedName( "fcItemChiselIron" );        
    }
    
    @Override
    public boolean GetCanBePlacedAsBlock()
    {
    	return true;
    }
    
    @Override
    public boolean onBlockDestroyed( ItemStack stack, World world, int iBlockID, int i, int j, int k, EntityLiving usingEntity )
    {
    	// extra damage for stump to workbench conversion
    	
        if ( iBlockID == Block.wood.blockID && world.getBlockId( i, j, k ) == 
        	FCBetterThanWolves.fcBlockWorkStump.blockID )
        {
            stack.damageItem( 10, usingEntity );
            
            return true;
        }

        return super.onBlockDestroyed( stack, world, iBlockID, i, j, k, usingEntity );
    }
    
    @Override
    public boolean IsDamagedInCrafting()
    {
    	// used to split stone into bricks, and bricks to loose stone
    	
    	return true;
    }
    
    @Override
    public void OnUsedInCrafting( EntityPlayer player, ItemStack outputStack )
    {
    	PlayStoneSplitSoundOnPlayer( player );
    	
		if ( !player.worldObj.isRemote && outputStack.itemID == 
			FCBetterThanWolves.fcItemStoneBrick.itemID )
		{
			// splitting full stone blocks into stone bricks
			
			FCUtilsItem.EjectStackWithRandomVelocity( player.worldObj, 
				player.posX, player.posY, player.posZ, 
				new ItemStack( FCBetterThanWolves.fcItemPileGravel, 2 ) );
		}
    }
    
    @Override
    public void OnBrokenInCrafting( EntityPlayer player )
    {
    	PlayBreakSoundOnPlayer( player );
    }

    @Override
    public boolean CanToolStickInBlock( ItemStack stack, Block block, World world, int i, int j, int k )
    {
		if ( block.blockMaterial == Material.rock && 
			block.blockID != Block.bedrock.blockID )
		{
			// ensures chisel will stick in cobble, despite not being efficient vs. it
			
			return true;
		}
    	
		return super.CanToolStickInBlock( stack, block, world, i, j, k );
    }
    
    //------------- Class Specific Methods ------------//
	
    static public void PlayStoneSplitSoundOnPlayer( EntityPlayer player )
    {
		if ( player.m_iTimesCraftedThisTick == 0 )
		{
			// note: the playSound function for player both plays the sound locally on the client, and plays it remotely on the server without it being sent again to the same player
			
			player.playSound( "random.anvil_land", 0.5F, player.worldObj.rand.nextFloat() * 
				0.25F + 1.75F );
		}
    }
    
    static public void PlayBreakSoundOnPlayer( EntityPlayer player )
    {
		// note: the playSound function for player both plays the sound locally on the client, and plays it remotely on the server without it being sent again to the same player
    	
		player.playSound( "random.break", 0.8F, 0.8F + player.worldObj.rand.nextFloat() * 0.4F );
    }
    
	//----------- Client Side Functionality -----------//
}
