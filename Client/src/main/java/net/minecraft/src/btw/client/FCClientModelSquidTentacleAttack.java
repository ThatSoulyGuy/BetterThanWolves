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


public class FCClientModelSquidTentacleAttack extends ModelBase
{
    public ModelRenderer m_modelRenderer;
    
    public FCClientModelSquidTentacleAttack()
    {
        m_modelRenderer = new ModelRenderer( this, 48, 0 );
        
        m_modelRenderer.addBox( -1.0F, 0F, -1.0F, 2, 16, 2 );
        
        m_modelRenderer.setRotationPoint( 0.0F, 7.6F, 0.0F );
    }
    
    public void render( Entity entity, float fPreScaleX, float fPreScaleY, float fPreScaleZ, float fYaw, float fPitch, float fBaseScale )
    {
        this.setRotationAngles( 0F, 0F, 0F, fYaw, fPitch, fBaseScale, entity );
        
        m_modelRenderer.RenderWithScaleToBaseModel( fBaseScale, fPreScaleX, fPreScaleY, fPreScaleZ );
    }
    
    public void setRotationAngles( float par1, float par2, float par3, float fYaw, float fPitch, float fBaseScale, Entity entity )
    {
        super.setRotationAngles( par1, par2, par3, fYaw, fPitch, fBaseScale, entity );
        
        m_modelRenderer.rotateAngleY = fYaw / (180F / (float)Math.PI);
        m_modelRenderer.rotateAngleX = fPitch / (180F / (float)Math.PI);
    }
}
