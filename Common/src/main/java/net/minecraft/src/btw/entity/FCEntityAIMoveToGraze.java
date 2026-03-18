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


public class FCEntityAIMoveToGraze extends EntityAIBase
{
    private EntityAnimal m_myAnimal;
    
    private float m_fMoveSpeed;
    
    public FCUtilsBlockPos m_destPos = new FCUtilsBlockPos();

    public FCEntityAIMoveToGraze( EntityAnimal entity, float fMoveSpeed )
    {
    	m_myAnimal = entity;
        m_fMoveSpeed = fMoveSpeed;
        
        setMutexBits( 1 );
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
    	if ( m_myAnimal.IsSubjectToHunger() )
    	{    		
    		if ( m_myAnimal.IsHungryEnoughToForceMoveToGraze() )
			{    			
		        return !m_myAnimal.ShouldStayInPlaceToGraze() &&
			    	FCUtilsRandomPositionGenerator.FindSimpleRandomTargetBlock( m_myAnimal, 
			    	10, 7, m_destPos );
			}
			else if ( m_myAnimal.getRNG().nextInt( 120 ) == 0 )
			{
		        return FCUtilsRandomPositionGenerator.FindSimpleRandomTargetBlock( m_myAnimal, 
	    			10, 7, m_destPos );
			}
    	}    	
    	else
    	{
    		return m_myAnimal.getRNG().nextInt( 120 ) == 0 &&
		    	FCUtilsRandomPositionGenerator.FindSimpleRandomTargetBlock( m_myAnimal, 
		    	10, 7, m_destPos );
    	}
    	
    	return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        return !m_myAnimal.getNavigator().noPath();
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
    	m_myAnimal.getNavigator().tryMoveToXYZ( m_destPos.i, m_destPos.j, m_destPos.k, 
    		m_fMoveSpeed );
    }
}
