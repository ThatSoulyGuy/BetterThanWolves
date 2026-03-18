package net.minecraft.src.btw.behavior;

import btw.api.*;
import net.minecraft.src.btw.core.*;
import net.minecraft.src.btw.block.*;
import net.minecraft.src.btw.item.*;
import net.minecraft.src.btw.entity.*;
import net.minecraft.src.btw.tileentity.*;
import net.minecraft.src.btw.crafting.*;
import net.minecraft.src.btw.api.*;
import net.minecraft.src.btw.util.*;
import net.minecraft.src.btw.world.*;
import net.minecraft.src.btw.behavior.*;
import net.minecraft.src.btw.properties.*;
import net.minecraft.src.btw.model.*;
import net.minecraft.src.btw.command.*;

// FCMOD



public class FCBehaviorSoulUrnDispense extends BehaviorProjectileDispense
{
    public FCBehaviorSoulUrnDispense()
    {
    }

    public IProjectile getProjectileEntity( World world, IPosition pos )
    {
        return new FCEntityUrn( world, pos.getX(), pos.getY(), pos.getZ(), FCBetterThanWolves.fcItemSoulUrn.itemID );
    }
}
