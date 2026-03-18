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


import java.util.Iterator;
import java.util.List;

public class FCEntityPig extends EntityPig
{
    public FCEntityPig( World world )
    {
    	super( world );
    	
        tasks.RemoveAllTasks();
        
        tasks.addTask( 0, new EntityAISwimming( this ) );
        tasks.addTask( 1, new FCEntityAIAnimalFlee( this, 0.38F ) );        
        tasks.addTask( 2, getAIControlledByPlayer() );
        tasks.addTask( 3, new EntityAIMate( this, 0.25F ) );
        tasks.addTask( 4, new FCEntityAIMultiTempt( this, 0.3F ) );
        tasks.addTask( 5, new EntityAIFollowParent( this, 0.28F ) );
        tasks.addTask( 6, new FCEntityAIGraze( this ) );
        tasks.addTask( 7, new FCEntityAIMoveToLooseFood( this, 0.25F ) );
        tasks.addTask( 8, new FCEntityAIMoveToGraze( this, 0.25F ) );        
        tasks.addTask( 9, new EntityAIWatchClosest( this, EntityPlayer.class, 6F ) );
        tasks.addTask( 10, new EntityAILookIdle( this ) );
    }

    @Override
    public boolean isAIEnabled()
    {
    	return !getWearingBreedingHarness();
    }

    @Override
    public void dropFewItems( boolean bKilledByPlayer, int iLootingModifier )
    {
        if ( !IsStarving() && !HasHeadCrabbedSquid() )
        {
	    	int iNumDrops = rand.nextInt( 2 ) + 1 + rand.nextInt( 1 + iLootingModifier );
	    	
	        if ( IsFamished() )
	        {
	        	iNumDrops = iNumDrops / 2;
	        }
	
	        for ( int iTempCount = 0; iTempCount < iNumDrops; ++iTempCount )
	        {
	            if ( isBurning() )
	            {
	                dropItem( FCBetterThanWolves.fcItemMeatBurned.itemID, 1 );
	            }
	            else
	            {
	                dropItem( Item.porkRaw.itemID, 1 );
	            }
	        }
        }

        if ( getSaddled() )
        {
            dropItem( Item.saddle.itemID, 1 );
        }
    }
	
	@Override
    public boolean attackEntityFrom( DamageSource damageSource, int par2)
    {
		// override to anger nearby pigmen
		
        if ( isEntityInvulnerable() )
        {
            return false;
        }
        
		Entity attackingEntity = damageSource.getEntity();

		if ( attackingEntity != null && ( attackingEntity instanceof EntityPlayer ) )
		{
			EntityPlayer attackingPlayer = (EntityPlayer)attackingEntity;
			
	        List pigmanList = worldObj.getEntitiesWithinAABB( FCEntityPigZombie.class, boundingBox.expand( 16D, 8D, 16D ) );
	        
	        Iterator itemIterator = pigmanList.iterator();
	
	        while ( itemIterator.hasNext())
	        {
	    		FCEntityPigZombie tempPigman = (FCEntityPigZombie)itemIterator.next();
	    		
		        if ( !tempPigman.isLivingDead )
		        {
		        	tempPigman.BecomeAngryWhenPigAttacked( attackingPlayer ); 
		        }		        
	        }
		}
		
        return super.attackEntityFrom( damageSource, par2 );
    }
	
    @Override
    public double getMountedYOffset()
    {
        if ( HasHeadCrabbedSquid() )
    	{
    		return (double)height * 1.2D;
    	}
    	
    	return super.getMountedYOffset();
    }
    
    @Override
    public boolean isBreedingItem( ItemStack itemStack )
    {
    	return itemStack.itemID == FCBetterThanWolves.fcItemChocolate.itemID;
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
        worldObj.playAuxSFX( FCBetterThanWolves.m_iPossessedPigTransformToPigmanAuxFXID, 
    		MathHelper.floor_double( posX ), MathHelper.floor_double( posY ), MathHelper.floor_double( posZ ), 
    		0 );
        
        setDead();
        
        FCEntityPigZombie entityPigman = new FCEntityPigZombie( worldObj );
        
        entityPigman.setLocationAndAngles( posX, posY, posZ, rotationYaw, rotationPitch );
        entityPigman.renderYawOffset = renderYawOffset;
        
        entityPigman.SetPersistent( true );
        
        entityPigman.setCanPickUpLoot( true );
        
        worldObj.spawnEntityInWorld( entityPigman );
    }
    
    @Override
    public boolean IsValidZombieSecondaryTarget( EntityZombie zombie )
    {
    	return true;
    }
    
    @Override
    public FCEntityPig spawnBabyAnimal( EntityAgeable parent )
    {
        return new FCEntityPig( worldObj );
    }
    
    @Override
    public void OnStruckByLightning( Entity bolt )
    {
        if ( !worldObj.isRemote )
        {
            FCEntityPigZombie pigman = new FCEntityPigZombie( worldObj );
            
            pigman.setLocationAndAngles( posX, posY, posZ, rotationYaw, rotationPitch );
            
            worldObj.spawnEntityInWorld( pigman );
            
            setDead();
        }
    }
    
    @Override
    public String getLivingSound()
    {
    	if ( !IsStarving() )
    	{
    		return "mob.pig.say";
    	}
    	else
    	{
    		return "mob.pig.death";
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
    	return 3;
    }    

    @Override
    public boolean GetDisruptsEarthOnGraze()
    {
    	return true;
    }
    
    @Override
    public boolean CanGrazeOnRoughVegetation()
    {
    	return true;
    }
    
    @Override
    public int GetGrazeDuration()
    {
    	return 80;
    }
    
    @Override
    public int GetItemFoodValue( ItemStack stack )
    {
    	return stack.getItem().GetPigFoodValue( stack.getItemDamage() ) * 
    		GetFoodValueMultiplier();
    }
    
    @Override
    public float GetGrazeHeadRotationMagnitudeDivisor()
    {
    	return 3F;
    }
    
    @Override
    public float GetGrazeHeadRotationRateMultiplier()
    {
    	return 28.7F * 1.75F;
    }
    
	//------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
