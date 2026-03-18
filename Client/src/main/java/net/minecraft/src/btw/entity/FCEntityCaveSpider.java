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


public class FCEntityCaveSpider extends FCEntitySpider
{
    public FCEntityCaveSpider( World world )
    {
        super( world );
        
        texture = "/mob/cavespider.png";
        
        setSize( 0.7F, 0.5F );
    }
    
    @Override
    public int getMaxHealth()
    {
        return 12;
    }

    @Override
    public boolean attackEntityAsMob( Entity target )
    {
        if ( super.attackEntityAsMob( target ) )
        {
            if ( target instanceof EntityLiving )
            {
                ((EntityLiving)target).addPotionEffect( new PotionEffect(
                	Potion.poison.id, 7 * 20, 0 ) );
            }

            return true;
        }
        
        return false;
    }

    @Override
    public void initCreature() 
    {
    	// skip spider jockey chance in parent    	
    }
    
    @Override
	public boolean DoesLightAffectAggessiveness()
	{
		return false;
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