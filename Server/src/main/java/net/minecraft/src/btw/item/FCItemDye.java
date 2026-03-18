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


public class FCItemDye extends ItemDye
{
    public FCItemDye( int iItemID )
    {
        super( iItemID );
        
        SetBellowsBlowDistance( 2 );
        
        setUnlocalizedName( "dyePowder" );
    }
    
    @Override
    public boolean onItemUse( ItemStack itemStack, EntityPlayer player, World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ )    
    {
        if ( itemStack.getItemDamage() == 15 ) // bone meal
        {        	
        	if ( ApplyBoneMeal( world, i, j, k ) )
        	{
                itemStack.stackSize--;
                
                return true;
        	}
        }

        return false;
    }

    @Override
    // client
    //public boolean itemInteractionForEntity( ItemStack stack, EntityLiving entity )
    // server
    public boolean useItemOnEntity( ItemStack stack, EntityLiving entity )
    {
        if ( entity instanceof FCEntitySheep )
        {
            FCEntitySheep sheep = (FCEntitySheep)entity;
            int i = BlockCloth.getBlockFromDye(stack.getItemDamage());

            if (!sheep.getSheared() && sheep.getFleeceColor() != i)
            {
            	sheep.setSuperficialFleeceColor(i);
            	
                stack.stackSize--;
            }

            return true;
        }
        
        return false;
    }
    
    @Override
    public int GetFilterableProperties( ItemStack stack )
    {
    	if ( stack.getItemDamage() == 0 )
    	{
    		// ink sack
    		
    		return m_iFilterable_Small;    		
    	}
    	
		return m_iFilterable_Fine;    		
    }
    
    //------------- Class Specific Methods ------------//

    private boolean ApplyBoneMeal( World world, int i, int j, int k )
    {    	
        Block targetBlock = Block.blocksList[world.getBlockId( i, j, k )];
        
        if ( targetBlock != null && 
        	targetBlock.AttemptToApplyFertilizerTo( world, i, j, k ) )
        {
            return true;
        }
        
    	return false;
    }
    
	//----------- Client Side Functionality -----------//
}