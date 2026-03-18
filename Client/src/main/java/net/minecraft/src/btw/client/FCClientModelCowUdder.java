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


public class FCClientModelCowUdder extends ModelQuadruped
{
    public FCClientModelCowUdder()
    {
        super(12, 0.0F);
        
        body = new ModelRenderer(this, 18, 4);
        
        body.setRotationPoint(0.0F, 5.0F, 2.0F);
        body.setTextureOffset(50, 0);
        
        body.addBox(-2.0F, 2.0F, -11.0F, 4, 6, 3 );
    }
}
