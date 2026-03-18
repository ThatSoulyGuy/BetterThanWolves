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


public class FCItemFoodHighRes extends FCItemFood
{
    public FCItemFoodHighRes( int iItemID, int iHungerHealed, float fSaturationModifier, boolean bWolfMeat, String sItemName )
    {
        super( iItemID, iHungerHealed, fSaturationModifier, bWolfMeat, sItemName );        
    }
    
    public FCItemFoodHighRes( int iItemID, int iHungerHealed, float fSaturationModifier, boolean bWolfMeat, String sItemName, boolean bZombiesConsume )
    {
        super( iItemID, iHungerHealed, fSaturationModifier, bWolfMeat, sItemName, bZombiesConsume );
    }
    
    @Override
    public int GetHungerRestored()
    {
    	// override of super function which multiplies the value
    	
    	return getHealAmount();
    }
}
