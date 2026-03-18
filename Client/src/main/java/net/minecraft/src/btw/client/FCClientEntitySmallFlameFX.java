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


public class FCClientEntitySmallFlameFX extends EntityFX
{
    private float m_fStartScale;

    public FCClientEntitySmallFlameFX( World world, double dXPos, double dYPos, double dZPos, 
    	double dXVel, double dYVel, double dZVel)
    {
        super( world, dXPos, dYPos, dZPos, dXVel, dYVel, dZVel );
        
        motionX = motionX * 0.009999999776482582D + dXVel;
        motionY = motionY * 0.009999999776482582D + dYVel;
        motionZ = motionZ * 0.009999999776482582D + dZVel;
        
        particleScale *= 0.5F;
        
        m_fStartScale = particleScale;
        
        particleMaxAge = (int)( 8D / ( Math.random() * 0.8D + 0.2D ) ) + 4;
        
        noClip = true;
        
        setParticleTextureIndex( 48 );
    }

    @Override
    public void renderParticle( Tessellator tesselator, float fPartialTicks, 
    	float par3, float par4, float par5, float par6, float par7)
    {
        float fDecay = GetDecay( fPartialTicks );
        
        particleScale = m_fStartScale * ( 1.0F - fDecay * fDecay * 0.5F );
        
        super.renderParticle( tesselator, fPartialTicks, par3, par4, par5, par6, par7 );
    }

    @Override
    public int getBrightnessForRender( float fPartialTicks )
    {
        float fDecay = GetDecay( fPartialTicks );

        int var3 = super.getBrightnessForRender( fPartialTicks );
        
        int var4 = var3 & 255;
        int var5 = var3 >> 16 & 255;
        
        var4 += (int)( fDecay * 15F * 16F );

        if ( var4 > 240 )
        {
            var4 = 240;
        }

        return var4 | var5 << 16;
    }

    public float getBrightness( float fPartialTicks )
    {
        float fDecay = GetDecay( fPartialTicks );

        float var3 = super.getBrightness( fPartialTicks );
        
        return var3 * fDecay + ( 1F - fDecay );
    }
    
	//------------- Class Specific Methods ------------//
    
    private float GetDecay( float fPartialTicks )
    {
        float fDecay = ( (float)particleAge + fPartialTicks ) / (float)particleMaxAge;

	    return MathHelper.clamp_float( fDecay, 0F, 1F );
    }
}
