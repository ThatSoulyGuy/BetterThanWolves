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


import java.util.Random;

public class FCItemFlintAndSteel extends FCItemFireStarter
{
	private static final float m_fFlintAndSteelExhaustionPerUse = 0.01F;
	
    public FCItemFlintAndSteel( int iItemID )
    {
    	super( iItemID, 64, m_fFlintAndSteelExhaustionPerUse );
    }
    
    @Override
    public boolean CheckChanceOfStart( ItemStack stack, Random rand )
    {
		return rand.nextInt( 4 ) == 0;
    }
    
    @Override
    public void PerformUseEffects( EntityPlayer player )
    {
        player.playSound( "fire.ignite", 1.0F, itemRand.nextFloat() * 0.4F + 0.8F );
    }
	
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
