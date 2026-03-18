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


import org.lwjgl.opengl.GL11;

public class FCClientRenderWolf extends RenderWolf
{
    public FCClientRenderWolf( ModelBase model, ModelBase modelOverlay, float fShadowSize )
    {
    	super( model, modelOverlay, fShadowSize );
    }
    
    @Override
    public int shouldRenderPass( EntityLiving entity, int iRenderPass, float par3 )
    {
    	if ( RenderGlowingEyes( (FCEntityWolf)entity, iRenderPass ) )
    	{
    		return 1;
    	}
    	
    	return super.shouldRenderPass( entity, iRenderPass, par3 );
    }
    
    //------------- Class Specific Methods ------------//
    
    private boolean RenderGlowingEyes( FCEntityWolf wolf, int iRenderPass )
    {
        if ( iRenderPass == 2 && wolf.AreEyesGlowing() )
        {
            this.loadTexture("/btwmodtex/fcWolfNothingToWorryAbout.png");
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            GL11.glDisable(GL11.GL_LIGHTING);

            if ( wolf.isInvisible() )
            {
                GL11.glDepthMask( false );
            }
            else
            {
                GL11.glDepthMask( true );
            }

            char var5 = 61680;
            int var6 = var5 % 65536;
            int var7 = var5 / 65536;
            
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)var6 / 1.0F, (float)var7 / 1.0F);
            
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            
        	return true;
        }
        
    	return false;
    }
    
}
