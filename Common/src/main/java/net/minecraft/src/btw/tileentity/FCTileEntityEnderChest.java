package net.minecraft.src.btw.tileentity;

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


public class FCTileEntityEnderChest extends TileEntityEnderChest
{
    private InventoryEnderChest m_localChestInventory = new InventoryEnderChest();
    
	@Override
    public void readFromNBT( NBTTagCompound tag)
    {
        super.readFromNBT( tag );
        
	    if ( tag.hasKey( "FCEnderItems" ) )
	    {
	        NBTTagList itemList = tag.getTagList( "FCEnderItems" );
	        
	    	m_localChestInventory.loadInventoryFromNBT( itemList );
	    }	    
    }

	@Override
    public void writeToNBT( NBTTagCompound tag )
    {
        super.writeToNBT( tag );
        
	    if ( m_localChestInventory != null )
	    {
	    	tag.setTag( "FCEnderItems", m_localChestInventory.saveInventoryToNBT() );
	    }	    
    }
	
    public InventoryEnderChest GetLocalEnderChestInventory()
    {
    	return m_localChestInventory;
    }	
}
