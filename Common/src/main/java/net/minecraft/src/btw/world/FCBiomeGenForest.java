package net.minecraft.src.btw.world;

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


public class FCBiomeGenForest extends BiomeGenForest
{
    public FCBiomeGenForest( int iBiomeID )
    {
        super( iBiomeID );
        
        spawnableCreatureList.clear();
        
        spawnableCreatureList.add( new SpawnListEntry( FCEntitySheep.class, 12, 4, 4 ) );
        spawnableCreatureList.add( new SpawnListEntry( FCEntityPig.class, 10, 4, 4 ) );
        spawnableCreatureList.add( new SpawnListEntry( FCEntityChicken.class, 10, 4, 4 ) );
        spawnableCreatureList.add( new SpawnListEntry( FCEntityCow.class, 8, 4, 4 ) );
        
        spawnableCreatureList.add( new SpawnListEntry( FCEntityWolf.class, 5, 4, 4 ) );
    }
}
