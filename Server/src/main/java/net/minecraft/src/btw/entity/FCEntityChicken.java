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


import java.util.List;

public class FCEntityChicken extends EntityChicken
{
    public long m_lTimeToLayEgg = 0;
    
    public FCEntityChicken( World world )
    {
        super( world );
        
        getNavigator().setAvoidsWater( true );
        
        tasks.RemoveAllTasks();
        
        tasks.addTask( 0, new EntityAISwimming( this ) );
        tasks.addTask( 1, new EntityAIPanic( this, 0.38F ) );
        tasks.addTask( 2, new EntityAIMate( this, 0.25F ) );
        tasks.addTask( 3, new FCEntityAIMultiTempt( this, 0.25F ) );
        tasks.addTask( 4, new EntityAIFollowParent( this, 0.28F ) );
        tasks.addTask( 5, new FCEntityAIGraze( this ) );
        tasks.addTask( 6, new FCEntityAIMoveToLooseFood( this, 0.25F ) );
        tasks.addTask( 7, new FCEntityAIMoveToGraze( this, 0.25F ) );
        tasks.addTask( 8, new EntityAIWatchClosest( this, EntityPlayer.class, 6F ) );
        tasks.addTask( 9, new EntityAILookIdle( this ) );
        
        renderDistanceWeight = 2D; // render further away than their size would normally allow
    }
    
    @Override
    public void onLivingUpdate()
    {
    	timeUntilNextEgg = 6000; // reset the vanilla egg laying counter so it never completes
    	
        super.onLivingUpdate();
    }

    @Override
    public void writeEntityToNBT( NBTTagCompound tag )
    {
        super.writeEntityToNBT( tag );
        
	 	tag.setLong( "fcTimeToLayEgg", m_lTimeToLayEgg );	    
    }
    
    @Override
    public void readEntityFromNBT( NBTTagCompound tag )
    {
        super.readEntityFromNBT( tag );

	    if ( tag.hasKey( "fcTimeToLayEgg" ) )
	    {
	    	m_lTimeToLayEgg = tag.getLong( "fcTimeToLayEgg" );
	    }
	    else
	    {
	    	m_lTimeToLayEgg = 0;
	    }
    }
    
    @Override
    public void playStepSound( int iBlockI, int iBlockJ, int iBlockK, int iBlockID )
    {
    	// Override to get rid of annoying clicking vanilla sound
    }
    
    @Override
    public void dropFewItems( boolean bKilledByPlayer, int iLootingModifier )
    {
    	if ( !IsStarving() )
    	{
	        int iNumDrops = rand.nextInt( 3 ) + rand.nextInt( 1 + iLootingModifier );
	
	        if ( IsFamished() )
	        {
	        	iNumDrops = iNumDrops / 2;
	        }
	
	        for ( int iTempCount = 0; iTempCount < iNumDrops; ++iTempCount )
	        {
	            dropItem( Item.feather.itemID, 1 );
	        }
	
	        if ( IsFullyFed() && !HasHeadCrabbedSquid() )
	        {
	            if ( isBurning() )
	            {
	                dropItem( FCBetterThanWolves.fcItemMeatBurned.itemID, 1 );
	            }
	            else
	            {
	                dropItem( Item.chickenRaw.itemID, 1 );
	            }
	        }
    	}
    }
    
    @Override
    public FCEntityChicken spawnBabyAnimal( EntityAgeable parent )
    {
        return new FCEntityChicken( worldObj );
    }
    
    @Override
    public boolean IsReadyToEatBreedingItem()
    {
    	return IsFullyFed() && getGrowingAge() == 0 && m_lTimeToLayEgg == 0;
    }
    
	@Override    
    public void OnEatBreedingItem()
    {
    	long lCurrentTime = FCUtilsWorld.GetOverworldTimeServerOnly();
    	
    	// following morning, at least half day from now
    	
    	m_lTimeToLayEgg = ( ( ( lCurrentTime + 12000L ) / 24000L ) + 1 ) * 24000L; 
    	
    	// crack of dawn (22550) + 30 seconds random variance
    	
    	m_lTimeToLayEgg +=  (long)( -1450 + rand.nextInt( 600 ) ); 
    	
		worldObj.playSoundAtEntity( this, 
        		getDeathSound(), getSoundVolume(), 
        		rand.nextFloat() * 0.2F + 1.5F );		
    }
    
	@Override
    public boolean IsAffectedByMovementModifiers()
    {
    	return false;
    }

    @Override
    public boolean GetCanCreatureTypeBePossessed()
    {
    	return true;
    }
    
	@Override
    public void OnFullPossession()
    {
        worldObj.playAuxSFX( FCBetterThanWolves.m_iPossessedChickenExplosionAuxFXID, 
    		MathHelper.floor_double( posX ), MathHelper.floor_double( posY ), MathHelper.floor_double( posZ ), 
    		0 );
        
        // explode feathers
        
        int iFeatherCount = rand.nextInt(3) + 2;

        for (int iTempCount = 0; iTempCount < iFeatherCount; iTempCount++)
        {
    		ItemStack itemStack = new ItemStack( Item.feather.itemID, 1, 0 );

        	double dFeatherX = posX + ( worldObj.rand.nextDouble() - 0.5D ) * 2D;
        	double dFeatherY = posY + 0.5D;
        	double dFeatherZ = posZ + ( worldObj.rand.nextDouble() - 0.5D ) * 2D;
        	
            EntityItem entityitem = new EntityItem( worldObj, dFeatherX, dFeatherY, dFeatherZ, itemStack );
            
            entityitem.motionX = ( worldObj.rand.nextDouble() - 0.5D ) * 0.5D;
            entityitem.motionY = 0.2D + worldObj.rand.nextDouble() * 0.3D;
            entityitem.motionZ = ( worldObj.rand.nextDouble() - 0.5D ) * 0.5D;
            
            entityitem.delayBeforeCanPickup = 10;
            
            worldObj.spawnEntityInWorld( entityitem );
        }
        
        AttemptToPossessNearbyCreatureOnDeath();
        
		setDead();
    }
        
    @Override
    public double getMountedYOffset()
    {
		return (double)height * 1.3F;
    }
    
    @Override
    public boolean isBreedingItem( ItemStack stack )
    {
        return stack.itemID == FCBetterThanWolves.fcItemChickenFeed.itemID;
    }
    
    @Override
    public String getLivingSound()
    {
    	if ( !IsStarving() )
    	{
    		return "mob.chicken.say";
    	}
    	else
    	{
    		return "mob.chicken.hurt";
    	}
    }

    @Override
    public boolean IsSubjectToHunger()
    {
    	return true;
    }
    
    @Override
    public int GetFoodValueMultiplier()
    {
    	return 1;
    }    

    @Override
    public boolean ShouldNotifyBlockOnGraze()
    {
    	// only clear a fraction of the blocks that we graze
    	
    	return rand.nextInt( 8 ) == 0;
    }

    @Override
    public void PlayGrazeFX( int i, int j, int k, int iBlockID )
    {
    	// chickens don't play block destroy effect since they are just pecking until
    	// the vegetation dies, rather than eating it
    }
    
    @Override
    public FCUtilsBlockPos GetGrazeBlockForPos()
    {
    	// chickens can't graze under snow and ash
    	
    	FCUtilsBlockPos pos = super.GetGrazeBlockForPos();
    	
    	if ( pos != null && FCBlockGroundCover.IsGroundCoverRestingOnBlock( worldObj, 
    		pos.i, pos.j, pos.k ) )
    	{
			return null;
    	}
    	
    	return pos;
    }
    
    @Override
    public int GetGrazeDuration()
    {
    	return 20;
    }
    
    @Override
    public boolean ShouldStayInPlaceToGraze()
    {
    	// wander off occasionally so they don't OCD the same spot
    	
    	if ( rand.nextInt( 10 ) != 0 )
    	{
    		return super.ShouldStayInPlaceToGraze();
    	}
    	
    	return false;
    }
    
    @Override
    public boolean IsHungryEnoughToForceMoveToGraze()
    {
    	// chickens are more frantic in their movements
    	
		return isChild() || !IsFullyFed() || m_iHungerCountdown + 
			( GetGrazeHungerGain() * 3 / 4 ) <= m_iFullHungerCount;
    }
    
    @Override
    public int GetItemFoodValue( ItemStack stack )
    {
    	return stack.getItem().GetChickenFoodValue( stack.getItemDamage() ) * 
    		GetFoodValueMultiplier();
    }
    
    @Override
    public void OnBecomeFamished()
    {
    	super.OnBecomeFamished();
    	
		// if the chicken is ever not in the fully fed state, it cancels egg production
		
		m_lTimeToLayEgg = 0;
    }
    
    @Override
    public void UpdateHungerState()
    {
        if ( !isChild() && IsFullyFed() && m_lTimeToLayEgg > 0 && ValidateTimeToLayEgg() )
        {
			// producing eggs consumes extra food. Hunger will be validated in super method

        	// Decided against extra hunger on chickens as I didn't think it would read well
			//m_iHungerCountdown--;
			
    		if ( FCUtilsWorld.GetOverworldTimeServerOnly() > m_lTimeToLayEgg )
    		{
	            playSound( "mob.slime.attack", 1.0F, getSoundPitch() );
	            
	            playSound( getDeathSound(), getSoundVolume(), ( getSoundPitch() + 0.25F ) * ( getSoundPitch() + 0.25F ) );
	            dropItem(Item.egg.itemID, 1);
	            
        		m_lTimeToLayEgg = 0;
    		}
        }
        
    	// must call super method after extra hunger consumed above to validate
    	
    	super.UpdateHungerState();
    }
    
	//------------- Class Specific Methods ------------//
    
    private boolean ValidateTimeToLayEgg()
    {
    	long lCurrentTime = FCUtilsWorld.GetOverworldTimeServerOnly();
    	long lDeltaTime = m_lTimeToLayEgg - lCurrentTime;
    	
    	if ( lDeltaTime > 48000L ) 
    	{
    		// we're more than 2 days before the time, something is wrong (like a time change command), so don't lay
    		
    		m_lTimeToLayEgg = 0;    		
    		
    		return false;
    	}
    	
    	return true;
    }

    @Override
    public float GetGrazeHeadRotationMagnitudeDivisor()
    {
    	return 3F;
    }
    
    @Override
    public float GetGrazeHeadRotationRateMultiplier()
    {
    	return 28.7F / 2F;
    }
    
	//----------- Client Side Functionality -----------//    
}
