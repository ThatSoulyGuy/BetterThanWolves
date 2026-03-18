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


public class FCEntityAIZombieSecondaryAttack extends EntityAIAttackOnCollide
{
	private EntityZombie m_zombie;
	
	public FCEntityAIZombieSecondaryAttack( EntityZombie zombie )
	{
		super( zombie, EntityCreature.class, zombie.moveSpeed, true );
		
		m_zombie = zombie;
	}
	
    public boolean continueExecuting()
    {
        EntityLiving var1 = this.attacker.getAttackTarget();
        
        if ( var1 == null || !var1.IsValidZombieSecondaryTarget( m_zombie ) )
        {
        	return false;
        }
        
        return super.continueExecuting();
    }
}
