package btw.forge.mixin;

import btw.forge.VillageInhabitants;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FCMOD-INTEGRATION(1.20.1): zombified_villages (live structure placement)
 * Feature / Conflict / Pattern / Injection strategy / Alt rejected: see INTEGRATIONS (#048). HEAD/RETURN of
 * StructureStart.placeInChunk (StructureStart.java:81-96, SRG m_226850_): when the level is a live ServerLevel
 * (/place structure, PlaceCommand.java:128-130; never a WorldGenRegion on a worldgen thread), the start is published in a
 * thread local, so VanillaMobReplacer can tell which village the villagers it is about to replace belong to; /place never
 * registers its start with the StructureManager.
 * Alt rejected: deciding /place'd villagers by position only (no village, so no 4.B rule); dropping /place support
 * (worldgen would then be the only path, and /place'd villages would hand out plain villagers).
 */
@Mixin(StructureStart.class)
public abstract class StructureStartPlacementMixin {

    // FCMOD-INTEGRATION(1.20.1): zombified_villages: a structure placed into a live level names its StructureStart to the villagers it spawns
    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void btw$enterPlacement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                    RandomSource random, BoundingBox box, ChunkPos chunk, CallbackInfo ci) {
        VillageInhabitants.enterLivePlacement(level, (StructureStart) (Object) this);
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void btw$exitPlacement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                                   RandomSource random, BoundingBox box, ChunkPos chunk, CallbackInfo ci) {
        VillageInhabitants.exitLivePlacement(level);
    }
}
