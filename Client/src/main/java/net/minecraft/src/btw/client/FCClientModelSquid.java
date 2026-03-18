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


public class FCClientModelSquid extends ModelSquid
{
    public FCClientModelSquid()
    {
    	super();
    }
    
    @Override
    public void render( Entity entity, float par2, float par3, float par4, float par5, float par6, float fScale )
    {
        setRotationAngles( par2, par3, par4, par5, par6, fScale, entity );
        
        squidBody.render( fScale );

        int iAttackTentacle = -1;
        
        if ( ((FCEntitySquid)entity).m_iTentacleAttackInProgressCounter > 0 )
        {
        	iAttackTentacle = 6;
        }
        
        for ( int iTempTentacle = 0; iTempTentacle < squidTentacles.length; ++iTempTentacle )
        {
        	if ( iTempTentacle != iAttackTentacle )
        	{
                squidTentacles[iTempTentacle].render( fScale );
        	}
        }
    }
}
