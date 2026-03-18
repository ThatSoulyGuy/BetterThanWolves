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


public class FCEntityAIPanicOnHeadCrab extends EntityAIBase
{
	private EntityCreature m_owningEntity;
	private float m_fMoveSpeed;
	
    private double m_fRandPosX;
    private double m_fRandPosY;
    private double m_fRandPosZ;
    
    public FCEntityAIPanicOnHeadCrab( EntityCreature entity, float fMoveSpeed )
    {
    	m_owningEntity = entity;
    	
        m_fMoveSpeed = fMoveSpeed;
        
        setMutexBits( 1 );
    }
    
    public boolean shouldExecute()
    {
    	if ( m_owningEntity.HasHeadCrabbedSquid() )
        {
            Vec3 randPos = RandomPositionGenerator.findRandomTarget( m_owningEntity, 5, 4 );

            if ( randPos != null )
            {
            	m_fRandPosX = randPos.xCoord;
            	m_fRandPosY = randPos.yCoord;
            	m_fRandPosZ = randPos.zCoord;
                
                return true;
            }
        }
    	
    	return false;
    }

    public void startExecuting()
    {
        m_owningEntity.getNavigator().tryMoveToXYZ( m_fRandPosX, m_fRandPosY, m_fRandPosZ, m_fMoveSpeed );
    }

    public boolean continueExecuting()
    {
        return !m_owningEntity.getNavigator().noPath() && m_owningEntity.HasHeadCrabbedSquid();
    }
}
