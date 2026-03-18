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


public class FCItemMushroomSoup extends ItemFood
{
    public FCItemMushroomSoup( int iItemID, int iHungerHealed )
    {
        super( iItemID, iHungerHealed, 0.25F, false );        
    }
    
    @Override
    public ItemStack onEaten( ItemStack stack, World world, EntityPlayer player )
    {
        super.onEaten( stack, world, player );

        ItemStack bowlStack = new ItemStack( Item.bowlEmpty );
        
        if ( !player.inventory.addItemStackToInventory( bowlStack ) )
        {
            player.dropPlayerItem( bowlStack );
        }
        
        return stack;
    }
}
