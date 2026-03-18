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


public class FCClientRenderSpider extends RenderSpider
{
    public FCClientRenderSpider()
    {
    	super();
    }
    
    @Override
    public int shouldRenderPass( EntityLiving entity, int iRenderPass, float par3)
    {
    	FCEntitySpider spider = (FCEntitySpider)entity;
    	
    	if ( !spider.DoEyesGlow() )
    	{
    		return -1;
    	}
    	
        return setSpiderEyeBrightness( spider, iRenderPass, par3 );
    }
}
