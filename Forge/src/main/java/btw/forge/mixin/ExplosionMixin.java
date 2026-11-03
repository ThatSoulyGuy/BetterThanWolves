package btw.forge.mixin;

import btw.forge.BlastRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_blast_rules
 * <p><b>Conflict:</b> 1.20.1 Explosion.finalizeExplosion (Explosion.java:226-286) drops every blasted block through its
 * loot table and its own reaction, so FC's per-block blast rules (canDropFromExplosion, 1/size drops, the 15
 * onBlockDestroyedByExplosion overrides) never ran (triage CF2-DR4).</p>
 * <p><b>Pattern:</b> D. HEAD: BlastRules runs 1.5.2 doExplosionB's body for the cells FC owns and leaves them air for
 * 1.20.1's loop; TAIL: FC's queued secondary explosions. Server thread only; KEEP explosions untouched.</p>
 * <p><b>Rebalance:</b> drop chance 1/radius when the blast decays, else 1 (decay gamerules; D1).</p>
 * <p><b>Rejected:</b> ExplosionEvent.Detonate (listener order against protection mods; before entity damage), redirects
 * of canDropFromExplosion/getDrops/onBlockExploded (exclusive, two coordinated redirects), a loot modifier on
 * EXPLOSION_RADIUS (misses non-decaying blasts; cannot run FC's reactions).</p>
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Shadow @Final private Level level;
    @Shadow @Final private float radius;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Final private boolean fire;

    @Unique private btw.modern.Explosion btw$fcExplosion;

    // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: FC's per-block blast rules first; the cells FC owns are air for 1.20.1's loop
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void btw$fcBlastRules(boolean spawnParticles, CallbackInfo ci) {
        btw$fcExplosion = level instanceof ServerLevel sl
                ? BlastRules.beforeFinalize((Explosion) (Object) this, sl, radius, blockInteraction, fire) : null;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: FC's queued secondary explosions after the blast, as 1.5.2's PerformSecondaryExplosions
    @Inject(method = "finalizeExplosion", at = @At("TAIL"))
    private void btw$fcSecondaryExplosions(boolean spawnParticles, CallbackInfo ci) {
        btw.modern.Explosion fc = btw$fcExplosion;
        btw$fcExplosion = null;
        if (fc != null && level instanceof ServerLevel sl) BlastRules.afterFinalize(sl, fc);
    }
}
