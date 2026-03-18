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


class FCInventoryInfernalEnchanter extends InventoryBasic
{
    final FCContainerInfernalEnchanter m_container;

    FCInventoryInfernalEnchanter( FCContainerInfernalEnchanter container, String name, int iNumSlots )
    {
        super( name, true, iNumSlots );
        
        m_container = container;
    }

    /*
    public int getInventoryStackLimit()
    {
        return 1;
    }
    */

    public void onInventoryChanged()
    {
        super.onInventoryChanged();
        
        m_container.onCraftMatrixChanged( this );
    }
}
