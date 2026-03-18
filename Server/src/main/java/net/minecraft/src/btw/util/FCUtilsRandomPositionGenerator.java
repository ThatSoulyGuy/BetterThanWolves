package net.minecraft.src.btw.util;

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

public class FCUtilsRandomPositionGenerator extends RandomPositionGenerator
{
    public static boolean FindSimpleRandomTargetBlock( EntityCreature entity, int iHorizontalRange, 
    	int iVerticalRange, FCUtilsBlockPos foundPos )
    {
    	// trimmed down version of parent function for better performance with the most
    	// common entities, that have no home to consider
    	
        Random rand = entity.getRNG();        
        boolean bFoundBlock = false;
        float fMaxFoundWeight = -99999F;
        
        int iMinX = MathHelper.floor_double( entity.posX ) - iHorizontalRange;
        int iMinY = (int)entity.posY - iVerticalRange;
        int iMinZ = MathHelper.floor_double( entity.posZ ) - iHorizontalRange;
        
        iHorizontalRange = iHorizontalRange * 2 + 1;
        iVerticalRange = iVerticalRange * 2 + 1;
        	
        for ( int iAttemptCount = 0; iAttemptCount < 10; iAttemptCount++ )
        {
            int iTempX = iMinX + rand.nextInt( iHorizontalRange );
            int iTempY = iMinY + rand.nextInt( iVerticalRange );
            int iTempZ = iMinZ + rand.nextInt( iHorizontalRange );

            float fTempWeight = entity.getBlockPathWeight(iTempX, iTempY, iTempZ);

            if ( fTempWeight > fMaxFoundWeight )
            {
                fMaxFoundWeight = fTempWeight;
                
                foundPos.i = iTempX;
                foundPos.j = iTempY;
                foundPos.k = iTempZ;
                
                bFoundBlock = true;
            }
        }

        return bFoundBlock;
    }
}
