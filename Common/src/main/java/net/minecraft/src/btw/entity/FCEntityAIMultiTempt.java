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


public class FCEntityAIMultiTempt extends EntityAIBase
{
    public EntityAnimal m_myAnimal;
    
    public float m_fMoveSpeed;
    
    private EntityPlayer temptingPlayer;

    private int m_delayBetweenCounter;
    
    private boolean m_bDoesAnimalNormallAvoidWater;

    public FCEntityAIMultiTempt( EntityAnimal animal, float fMoveSpeed )
    {
        m_delayBetweenCounter = 0;
        
        m_myAnimal = animal;
        m_fMoveSpeed = fMoveSpeed;
        
        setMutexBits( 3 );
    }

    public boolean shouldExecute()
    {
        if ( m_delayBetweenCounter <= 0 )
        {
	        temptingPlayer = m_myAnimal.worldObj.getClosestPlayerToEntity( m_myAnimal, 10D );
	
	        if ( temptingPlayer != null )
	        {
		        ItemStack itemstack = temptingPlayer.getCurrentEquippedItem();
		
		        if ( itemstack != null )
		        {
			        return m_myAnimal.IsTemptingItem( itemstack );
		        }		
	        }
        }
        else
        {
            m_delayBetweenCounter--;
        }
        
        return false;
    }

    public void startExecuting()
    {
        m_bDoesAnimalNormallAvoidWater = m_myAnimal.getNavigator().getAvoidsWater();
        m_myAnimal.getNavigator().setAvoidsWater( false );
    }

    public void resetTask()
    {
        temptingPlayer = null;
        m_myAnimal.getNavigator().clearPathEntity();
        m_delayBetweenCounter = 33; // note that AI is only checked every 3 ticks
        
        m_myAnimal.getNavigator().setAvoidsWater( m_bDoesAnimalNormallAvoidWater );
    }

    public void updateTask()
    {
        m_myAnimal.getLookHelper().setLookPositionWithEntity( temptingPlayer, 30F, 
        	m_myAnimal.getVerticalFaceSpeed() );

        if ( m_myAnimal.getDistanceSqToEntity( temptingPlayer ) < 6.25D )
        {
            m_myAnimal.getNavigator().clearPathEntity();
        }
        else
        {
            m_myAnimal.getNavigator().tryMoveToEntityLiving( temptingPlayer, m_fMoveSpeed );
        }
    }
}
