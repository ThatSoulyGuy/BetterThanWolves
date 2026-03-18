package net.minecraft.src.btw.entity;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.client.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD


public class FCEntityJungleSpider extends FCEntitySpider
{
    public FCEntityJungleSpider( World world )
    {
        super( world );
        
        texture = "/btwmodtex/fcSpiderJungle.png";
        
        setSize( 0.7F, 0.5F );
    }

    @Override
    public int getMaxHealth()
    {
        return 10;
    }

    @Override
    public boolean attackEntityAsMob( Entity targetEntity )
    {
        if ( super.attackEntityAsMob( targetEntity ) )
        {
            if ( targetEntity instanceof EntityLiving )
            {
                byte iPoisonDurationInSeconds = 0;

                if (this.worldObj.difficultySetting > 1)
                {
                    if (this.worldObj.difficultySetting == 2)
                    {
                        iPoisonDurationInSeconds = 3;
                    }
                    else if (this.worldObj.difficultySetting == 3)
                    {
                        iPoisonDurationInSeconds = 7;
                    }
                }
                else
                {
                	iPoisonDurationInSeconds = 1;
                }

                if (iPoisonDurationInSeconds > 0)
                {
                    ((EntityLiving)targetEntity).addPotionEffect( new PotionEffect( Potion.poison.id, iPoisonDurationInSeconds * 20, 0 ) );
                    
                    ((EntityLiving)targetEntity).addPotionEffect( new PotionEffect( Potion.hunger.id, 30 * 20, 0 ) );                    
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void initCreature() 
    {
    	// override so you don't get jockeys    	
    }
    
    @Override
	public boolean DoesLightAffectAggessiveness()
	{
		return false;
	}
    
    @Override
    public boolean isValidLightLevel()
    {
    	// spawn in any light level
    	
    	return true;
    }
    
    @Override
    public boolean getCanSpawnHere()
    {
    	// limit spawns to above and around sea level
    	
    	if ( (int)posY >= worldObj.provider.getAverageGroundLevel() - 5 )
    	{
    		return super.getCanSpawnHere();
    	}
    	
    	return false;
    }
    
    @Override
    public String getEntityName()
    {
    	return "Jungle Spider";
    }
    
    @Override
    public boolean CanSpawnOnBlock( int iBlockID )
    {
    	return iBlockID == Block.leaves.blockID;
    }

    @Override
    public float getBlockPathWeight( int i, int j, int k )
    {
    	// jungle spiders love hanging out (and spawning) on leaves
    	
    	if ( worldObj.getBlockId(i, j - 1, k) == Block.leaves.blockID )
    	{
    		return 10F;
    	}
    	
        return super.getBlockPathWeight(i, j, k);
    }
    
    @Override
    public float getSoundPitch()
    {
        return ( rand.nextFloat() - rand.nextFloat() ) * 0.2F + 0.7F;
    }
    
    @Override
    public float getSoundVolume()
    {
        return 0.25F;
    }
    
    @Override
    public int getTalkInterval()
    {
        return 80 + rand.nextInt( 240 ); // default is 80
    }
    
    @Override
	public boolean DoEyesGlow()
	{
		return false;
	}
    
    @Override
    public boolean attackEntityFrom( DamageSource damageSource, int iDamageAmount )
    {
    	// make immune to fall damage 
    	
        if ( damageSource == DamageSource.fall )
        {
        	return false;
        }
        
    	return super.attackEntityFrom( damageSource, iDamageAmount );
    }

    @Override
    public void dropFewItems( boolean bKilledByPlayer, int iLootingModifier )
    {
        super.dropFewItems( bKilledByPlayer, iLootingModifier );

        if ( rand.nextInt( 16 ) - ( iLootingModifier << 1 ) <= 0 )
        {
        	// drop venom sacks
        	
            dropItem( Item.fermentedSpiderEye.itemID, 1 );
        }
    }
    
    @Override
    public boolean DropsSpiderEyes()
    {
    	return false;
    }
    
    @Override
	public void CheckForSpiderSkeletonMounting()
	{
	}
	
	//----------- Client Side Functionality -----------//
	
    @Override
    public float spiderScaleAmount()
    {
        return 0.7F;
    }
}
