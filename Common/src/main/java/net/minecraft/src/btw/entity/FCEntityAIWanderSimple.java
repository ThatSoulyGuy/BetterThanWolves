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

// FCMOD: A simplified version of EntityAIWander that uses int destinations to avoid multiple
// typecasts, and which ignores the entities "home" position entirely, since most creatures
// don't even have one.


public class FCEntityAIWanderSimple extends EntityAIBase
{
    private EntityCreature m_myEntity;
    
    private float m_fMoveSpeed;
    
    public FCUtilsBlockPos m_destPos = new FCUtilsBlockPos();

    public FCEntityAIWanderSimple( EntityCreature entity, float fMoveSpeed )
    {
        m_myEntity = entity;
        m_fMoveSpeed = fMoveSpeed;
        
        setMutexBits( 1 );
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        if ( m_myEntity.getRNG().nextInt( 120 ) == 0 &&
        	FCUtilsRandomPositionGenerator.FindSimpleRandomTargetBlock( m_myEntity, 
        	10, 7, m_destPos ) )
        {
            return true;
        }
        
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        return !m_myEntity.getNavigator().noPath();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
    	m_myEntity.getNavigator().tryMoveToXYZ( m_destPos.i, m_destPos.j, m_destPos.k, 
    		m_fMoveSpeed );
    }
}
