package net.minecraft.src.btw.client;

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

// FCMOD (client only)


public class FCClientModelChicken extends ModelChicken
{
    private float m_fHeadRotation;
    
    public FCClientModelChicken()
    {
        super();
    }

    @Override    
    public void setLivingAnimations( EntityLiving entity, float par2, float par3, 
    	float fPartialTick )
    {
        super.setLivingAnimations( entity, par2, par3, fPartialTick );

        FCEntityChicken chicken = (FCEntityChicken)entity;
        
        if ( !chicken.isChild() )
        {
        	head.rotationPointY = 15F + chicken.GetGrazeHeadVerticalOffset( fPartialTick ) * 3F;
        }
        else
        {        	
        	head.rotationPointY = 15F + chicken.GetGrazeHeadVerticalOffset( fPartialTick ) * 1.5F;
        }
        	
        bill.rotationPointY = chin.rotationPointY = head.rotationPointY;
        
        m_fHeadRotation = chicken.GetGrazeHeadRotation( fPartialTick );
    }

    @Override    
    public void setRotationAngles( float par1, float par2, float par3, 
    	float par4, float par5, float par6, Entity entity )
    {
        super.setRotationAngles( par1, par2, par3, par4, par5, par6, entity );
        
        head.rotateAngleX = bill.rotateAngleX = chin.rotateAngleX = m_fHeadRotation;
    }
    
	//------------- Class Specific Methods ------------//
}
