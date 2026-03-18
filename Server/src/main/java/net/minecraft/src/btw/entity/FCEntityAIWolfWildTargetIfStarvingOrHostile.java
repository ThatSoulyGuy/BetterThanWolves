package net.minecraft.src.btw.entity;

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


public class FCEntityAIWolfWildTargetIfStarvingOrHostile extends EntityAINearestAttackableTarget
{
    private FCEntityWolf m_AssociatedWolf;

    public FCEntityAIWolfWildTargetIfStarvingOrHostile( FCEntityWolf wolf, Class targetClass, float fTargetRange, int iChanceOfTargeting, boolean bCheckLineOfSight )
    {
        super( wolf, targetClass, fTargetRange, iChanceOfTargeting, bCheckLineOfSight );
        
        m_AssociatedWolf = wolf;
    }

    @Override
    public boolean continueExecuting()
    {
    	if ( !m_AssociatedWolf.IsWildAndHostile() )
    	{
    		return false;
    	}
    	
    	return super.continueExecuting();
    }
    
    @Override
    public boolean shouldExecute()
    {
    	if ( !m_AssociatedWolf.IsWildAndHostile() )
    	{
    		return false;
    	}
    	
        return super.shouldExecute();
    }
}
