package net.minecraft.src.btw.crafting;

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


import java.util.Iterator;

public class FCContainerHamper extends FCContainerWithInventory
{
	private static final int m_iInvetoryRows = 2;
	private static final int m_iInvetoryColumns = 2;
	
	private static final int m_iContainerInventoryDisplayX = 71;
	private static final int m_iContainerInventoryDisplayY = 17;
	
	private static final int m_iPlayerInventoryDisplayX = 8;
	private static final int m_iPlayerInventoryDisplayY = 67;
	
    public FCContainerHamper( IInventory playerInventory, IInventory hamperInventory )
    {
    	super( playerInventory, hamperInventory, m_iInvetoryRows, m_iInvetoryColumns, 
    		m_iContainerInventoryDisplayX, m_iContainerInventoryDisplayY, 
    		m_iPlayerInventoryDisplayX, m_iPlayerInventoryDisplayY );
    	
		hamperInventory.openChest();
    }
    
    @Override
    public void onCraftGuiClosed( EntityPlayer player )
    {
        super.onCraftGuiClosed( player );
        
		m_containerInventory.closeChest();
    }
}