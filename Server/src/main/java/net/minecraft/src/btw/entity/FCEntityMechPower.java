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


public abstract class FCEntityMechPower extends Entity
	implements FCIEntityPacketHandler
{
	// constants
	
	private static final int m_iRotationSpeedDataWatcherID = 22;
	
	// local vars
	
    public float m_fRotation;
    public int m_iCurrentDamage;
    public int m_iTimeSinceHit;
    public int m_iRockDirection;
    
    public boolean m_bProvidingPower;
    public int m_iFullUpdateTickCount;
    
    public FCEntityMechPower( World world )
    {
        super( world );
        
        m_bProvidingPower = false;
        
        m_iCurrentDamage = 0;
        m_iTimeSinceHit = 0;
        m_iRockDirection = 1;
        
        m_fRotation = 0F;
        
        m_iFullUpdateTickCount = 0;
        
        preventEntitySpawning = true;
        
        setSize( GetWidth(), GetHeight() );
        
        yOffset = height / 2.0F;        
    }
    
    public FCEntityMechPower( World world, double x, double y, double z  ) 
    {
        this( world );
        
        setPosition( x, y, z );        
    }
    
    @Override
    public void entityInit()
    {
        dataWatcher.addObject( m_iRotationSpeedDataWatcherID, new Integer( 0 ) );
    }
    
	@Override
    public boolean canTriggerWalking()
    {
        return false;
    }

	@Override
    public AxisAlignedBB getCollisionBox(Entity entity)
    {
        return entity.boundingBox;
    }

	@Override
    public AxisAlignedBB getBoundingBox()
    {
        return boundingBox;
    }

	@Override
    public boolean canBePushed()
    {
        return false;
    }

	@Override
    public boolean canBeCollidedWith()
    {
        return !isDead;
    }

	@Override
    public void moveEntity( double deltaX, double deltaY, double deltaZ )
    {
    	// this might be called by external sources (like the pistons), so we have to override it
    	// destroy the device if it is moved by an external source
    	
    	if ( !isDead )
    	{
    		DestroyWithDrop();
    	}
    }
    
	@Override
    public void setFire( int i )
    {
		// stub to prevent this entity from catching fire as it is so large that the fire effect would just look fucked
    }
	
	@Override
    public boolean attackEntityFrom( DamageSource damageSource, int i )
    {
        if ( isDead )
        {
            return true;
        }
        
        // Note: the server and client can have differing damage values as the server will destroy the ent when it takes too much, and the rest is just visual
        
        m_iCurrentDamage += i * 5; 
        m_iRockDirection = -m_iRockDirection;
        m_iTimeSinceHit = 10;
        
        if ( !worldObj.isRemote )
        {        
	        Entity attackingEntity = damageSource.getEntity();
	        	
	        if ( (attackingEntity instanceof EntityPlayer) && ((EntityPlayer)attackingEntity).capabilities.isCreativeMode )
	        {
	        	// destroy on first hit in creative
	        	
	        	DestroyWithDrop();
	        }
	        else
	        {
		        setBeenAttacked();
		        
		        if ( m_iCurrentDamage > GetMaxDamage() )
		        {
		        	DestroyWithDrop();
		        }
	        }
        }
        
    	return true;
    }
    
	@Override
    public void onUpdate()
    {   
		// intentionally doesn't call super method
		
    	if ( isDead )
    	{
    		return;
    	}
    	
    	if ( !worldObj.isRemote )
    	{
    		m_iFullUpdateTickCount--;
	        
	        if ( m_iFullUpdateTickCount <= 0 )
	        {
	        	m_iFullUpdateTickCount = GetTicksPerFullUpdate();
	        	
	        	OnFullUpdateServer();	        	
	        }
	        
	        UpdateRotationAndDamageState();
    	}
    	else
    	{
        	float m_fPrevRotation = m_fRotation;
        	
	        UpdateRotationAndDamageState();
	        
	    	int iNewOctant = (int)( m_fRotation / 45F );
	    	int iOldOctant = (int)( m_fPrevRotation / 45F );	
	    	
	    	if ( iOldOctant != iNewOctant )
	    	{
	    		OnClientRotationOctantChange();
	    	}
    	}    	
    }
	
    @Override
    public boolean ShouldSetPositionOnLoad()
    {
    	return false;
    }
    
    @Override
    public boolean AttractsLightning()
    {
    	return true;
    }
    
    //************* FCIEntityPacketHandler ************//

    @Override
    public int GetTrackerViewDistance()
    {
    	return 160;
    }
    
    @Override
    public int GetTrackerUpdateFrequency()
    {
    	return 3;
    }
    
    @Override
    public boolean GetTrackMotion()
    {
    	return false;
    }
    
    @Override
    public boolean ShouldServerTreatAsOversized()
    {
    	return true;
    }
    
	//------------- Class Specific Methods ------------//

    public abstract float GetWidth();
    
    public abstract float GetHeight();
    
    public abstract float GetDepth();
    
    public abstract void InitBoundingBox();
    
    public abstract AxisAlignedBB GetDeviceBounds();
    
    public abstract int GetMaxDamage();
    
    public abstract int GetTicksPerFullUpdate();
    
    public abstract void DestroyWithDrop();
    
    public abstract boolean ValidateAreaAroundDevice();
    
    public abstract boolean ValidateConnectedAxles();    
    
    public abstract float ComputeRotation();
    
    public abstract void TransferPowerStateToConnectedAxles();
    
	private void UpdateRotationAndDamageState()
	{
        // update rotation
        
    	m_fRotation += getRotationSpeed();
    	
    	if ( m_fRotation > 360F )
    	{
    		m_fRotation -= 360;
    	}
    	else if ( m_fRotation < -360F )
    	{
    		m_fRotation += 360F;
    	}
    	
    	// update damage state
    	
        if ( m_iTimeSinceHit > 0 )
        {
        	m_iTimeSinceHit--;
        }
        
        if ( m_iCurrentDamage > 0 )
        {
        	m_iCurrentDamage--;
        }        
	}
	
	public void OnClientRotationOctantChange()
	{
	}
	
    public boolean IsClearOfBlockingEntities()
    {
    	AxisAlignedBB deviceBounds = GetDeviceBounds();
    	
    	return worldObj.checkNoEntityCollision( deviceBounds, this );    	
    }
    
    public float getRotationSpeed()
    {
        return (float)( dataWatcher.getWatchableObjectInt( m_iRotationSpeedDataWatcherID ) ) / 100F;
    }
    
    public void setRotationSpeed( float fRotation )
    {
        dataWatcher.updateObject( m_iRotationSpeedDataWatcherID, Integer.valueOf( (int)( fRotation * 100F ) ) );
    }
    
    public int getRotationSpeedScaled()
    {
        return dataWatcher.getWatchableObjectInt( m_iRotationSpeedDataWatcherID );
    }
    
    public void setRotationSpeedScaled( int iRotationSpeedScaled )
    {
        dataWatcher.updateObject( m_iRotationSpeedDataWatcherID, Integer.valueOf( iRotationSpeedScaled ) );
    }
    
    public void OnFullUpdateServer()
    {
    	// validate the entity is still occupying a valid location
    	
    	if ( !ValidateAreaAroundDevice() || !ValidateConnectedAxles()  )
    	{
    		DestroyWithDrop();
    		
    		return;
    	}    	
    	
        // update the rotation;
        
        setRotationSpeed( ComputeRotation() );
        
        float fCurrentSpeed = getRotationSpeed();
        
        boolean bNewPoweredState = false;

        if ( fCurrentSpeed > 0.01F || fCurrentSpeed < -0.01F )
        {
        	bNewPoweredState = true;
        }
        
    	if ( m_bProvidingPower != bNewPoweredState )
    	{	    		
    		m_bProvidingPower = bNewPoweredState;
    		
    		TransferPowerStateToConnectedAxles();    		
    	}
    }
    
	//----------- Client Side Functionality -----------//
}