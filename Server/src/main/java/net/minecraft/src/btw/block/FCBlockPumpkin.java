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


public class FCBlockPumpkin extends FCBlockGourd
{
    public FCBlockPumpkin( int iBlockID, boolean bStub )
    {
        super( iBlockID );        
        
        setHardness(1.0F);
        
        setStepSound(soundWoodFootstep);
        
        setUnlocalizedName("fcBlockPumpkinFresh");    
    }

    //------------- FCBlockGourd ------------//
    
    @Override
	public Item ItemToDropOnExplode()
    {
    	return Item.pumpkinSeeds;
    }
	
    @Override
	public int ItemCountToDropOnExplode()
    {
    	return 4;
    }
	
    @Override
	public int AuxFXIDOnExplode()
    {
    	return FCBetterThanWolves.m_iPumpkinExplodeAuxFXID;
    }
    
	public DamageSource GetFallDamageSource()
	{
		return FCDamageSourceCustom.m_DamageSourcePumpkin;
	}
}
