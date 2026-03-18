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



import java.nio.ByteBuffer;
import java.util.List;

public class FCClientTextureFire extends TextureStitched
{
    ByteBuffer m_frameBuffer;
    
    int m_iBufferWidth;
    int m_iBufferHeight;
    int m_iBufferPixelSize;
    
    int m_iFireAnimationIndex;
    FCClientAnimationFire m_FireAnimation = null;

    public FCClientTextureFire( String sName, int iFireAnimationIndex )
    {
        super( sName );
        
        m_iFireAnimationIndex = iFireAnimationIndex;
    }
    
    @Override
    public void init(Texture par1Texture, List par2List, int par3, int par4, int par5, int par6, boolean par7)
    {
    	super.init(par1Texture, par2List, par3, par4, par5, par6, par7);
    	
        m_iBufferWidth = ((Texture)this.textureList.get(0)).getWidth();
        m_iBufferHeight = ((Texture)this.textureList.get(0)).getHeight();
        m_iBufferPixelSize = m_iBufferWidth * m_iBufferHeight;
        
        m_frameBuffer = ByteBuffer.allocateDirect( m_iBufferPixelSize * 4 );
        
        m_FireAnimation = FCClientAnimationFire.m_InstanceArray[m_iFireAnimationIndex];
        
        if ( m_FireAnimation == null )
        {
        	m_FireAnimation = new FCClientAnimationFire( m_iFireAnimationIndex, m_iBufferWidth, m_iBufferHeight );
        }
    }

    @Override
    public void updateAnimation()
    {
    	frameCounter = 0;

        if ( m_FireAnimation != null )
        {
        	m_FireAnimation.CopyRegularFireFrameToByteBuffer( m_frameBuffer, m_iBufferPixelSize );
        }
    	
    	textureSheet.UploadByteBufferToGPU( originX, originY, m_frameBuffer, m_iBufferWidth, m_iBufferHeight );
    }
    
    @Override
    public boolean IsProcedurallyAnimated()
    {
    	return true;
    }    
}
