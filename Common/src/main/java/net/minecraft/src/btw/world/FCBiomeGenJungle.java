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


import java.util.Random;

public class FCBiomeGenJungle extends BiomeGenJungle
{
	private static final int m_iExtraReedsPerChunk = 100;
	
    public FCBiomeGenJungle( int iBiomeID )
    {
        super( iBiomeID );
        
        spawnableCreatureList.clear();
        
        spawnableCreatureList.add( new SpawnListEntry( FCEntityChicken.class, 10, 4, 4 ) );
        spawnableCreatureList.add( new SpawnListEntry( FCEntityPig.class, 10, 4, 4 ) );
        spawnableCreatureList.add( new SpawnListEntry( FCEntityChicken.class, 10, 4, 4 ) );

        spawnableMonsterList.clear();
        
        spawnableMonsterList.add( new SpawnListEntry( FCEntityJungleSpider.class, 2, 1, 1 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntitySpider.class, 10, 4, 4 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntityZombie.class, 10, 4, 4 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntitySkeleton.class, 10, 4, 4 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntityCreeper.class, 10, 4, 4 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntitySlime.class, 10, 4, 4 ) );
        spawnableMonsterList.add( new SpawnListEntry( FCEntityEnderman.class, 1, 1, 4 ) );        
        spawnableMonsterList.add( new SpawnListEntry( FCEntityOcelot.class, 2, 1, 1 ) );
    }
    
    @Override
    public void decorate( World world, Random rand, int iChunkX, int iChunkZ )
    {
        super.decorate( world, rand, iChunkX, iChunkZ );
        
        // separate reed generation after other decorations to avoid messing with other random elements
        
        for ( int iTempCount = 0; iTempCount < m_iExtraReedsPerChunk; ++iTempCount )
        {
            int iXGen = iChunkX + rand.nextInt( 16 ) + 8;
            int iZGen = iChunkZ + rand.nextInt( 16 ) + 8;
            
            int iYGen = rand.nextInt( 128 );
            
            theBiomeDecorator.reedGen.generate( world, rand, iXGen, iYGen, iZGen);
        }
    }
}