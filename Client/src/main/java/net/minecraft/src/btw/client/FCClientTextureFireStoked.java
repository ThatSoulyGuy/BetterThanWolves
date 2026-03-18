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

public class FCClientTextureFireStoked extends FCClientTextureFire
{
    public FCClientTextureFireStoked( String sName, int iFireAnimationIndex )
    {
        super( sName, iFireAnimationIndex );
        
        m_iFireAnimationIndex = iFireAnimationIndex;
    }
    
    @Override
    public void updateAnimation()
    {
    	frameCounter = 0;

        if ( m_FireAnimation != null )
        {
        	m_FireAnimation.CopyStokedFireFrameToByteBuffer( m_frameBuffer, m_iBufferPixelSize );
        }
    	
    	textureSheet.UploadByteBufferToGPU( originX, originY, m_frameBuffer, m_iBufferWidth, m_iBufferHeight );
    }
    
    @Override
    public boolean IsProcedurallyAnimated()
    {
    	return true;
    }    
}
