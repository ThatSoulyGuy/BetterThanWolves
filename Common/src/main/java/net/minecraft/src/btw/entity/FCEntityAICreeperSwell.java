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


public class FCEntityAICreeperSwell extends EntityAICreeperSwell
{
    private FCEntityCreeper m_myCreeper;

    public FCEntityAICreeperSwell( FCEntityCreeper creeper )
    {
    	super( creeper );
    	
    	m_myCreeper = creeper;
    }

    @Override
    public boolean shouldExecute()
    {
    	if ( m_myCreeper.getCreeperState() <= 0 && m_myCreeper.GetNeuteredState() > 0 )
    	{
    		return false;
    	}
    	else if ( m_myCreeper.GetIsDeterminedToExplode() )
    	{
    		return true;
    	}
    	
    	return super.shouldExecute();
    }

    @Override
    public void updateTask()
    {
    	if ( m_myCreeper.GetNeuteredState() > 0 )
    	{
    		m_myCreeper.setCreeperState( -1 );
    	}
    	else if ( !m_myCreeper.GetIsDeterminedToExplode() &&
			( creeperAttackTarget == null || m_myCreeper.getDistanceSqToEntity(this.creeperAttackTarget) > 36D ||
    		!m_myCreeper.getEntitySenses().canSee(creeperAttackTarget) ) )
    	{
    		m_myCreeper.setCreeperState( -1 );
    	}    	
        else
        {
        	m_myCreeper.setCreeperState( 1 );
        }
    }
}
