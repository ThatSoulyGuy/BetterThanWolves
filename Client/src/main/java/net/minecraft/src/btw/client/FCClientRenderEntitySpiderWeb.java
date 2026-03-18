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

// FCMOD


import org.lwjgl.opengl.GL11;

public class FCClientRenderEntitySpiderWeb extends Render
{
	private static final int m_iIconIndex = 111;
	
    public FCClientRenderEntitySpiderWeb()
    {
    }

    @Override
    public void doRender( Entity entity, double d, double d1, double d2, 
            float f, float f1 )
    {
        loadTexture( "/btwmodtex/fcSpiderSpit.png" );
        
        Tessellator tessellator = Tessellator.instance;
        
        GL11.glPushMatrix();
        GL11.glTranslatef((float)d, (float)d1, (float)d2);
        GL11.glEnable( 32826 ); //GL_RESCALE_NORMAL_EXT
        GL11.glScalef( 0.5F, 0.5F, 0.5F );        
        
        GL11.glRotatef(180F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        
        tessellator.startDrawingQuads();
        
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        
        float fUMin = 0F;
        float fUMax = 1F;
        float fVMin = 0F;
        float fVMax = 1F;
        
        float fXOffset = 0.25F;
        float fYOffset = 0.25F;
        
        tessellator.addVertexWithUV( 0.0F - fXOffset, 0.0F - fYOffset, 0.0D, fUMin, fVMax);
        tessellator.addVertexWithUV( 1.0F - fXOffset, 0.0F - fYOffset, 0.0D, fUMax, fVMax);
        tessellator.addVertexWithUV( 1.0F - fXOffset, 1.0F - fYOffset, 0.0D, fUMax, fVMin);
        tessellator.addVertexWithUV( 0.0F - fXOffset, 1.0F - fYOffset, 0.0D, fUMin, fVMin);
        
        tessellator.draw();
        
        GL11.glDisable(32826); //GL_RESCALE_NORMAL_EXT
        GL11.glPopMatrix();
    }
}