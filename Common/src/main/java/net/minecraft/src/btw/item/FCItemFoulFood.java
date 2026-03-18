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


public class FCItemFoulFood extends FCItemFood
{
	static private final int m_iHealthHealed = 1;
	static private final float m_iSaturationModifier = 0F;

    public FCItemFoulFood( int iItemID )
    {
        super( iItemID, m_iHealthHealed, m_iSaturationModifier, false, "fcItemFoulFood" );
        
        SetIncreasedFoodPoisoningEffect();
    }    
}
