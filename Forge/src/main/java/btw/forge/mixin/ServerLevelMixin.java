package btw.forge.mixin;

import btw.forge.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Hooks into {@link ServerLevel#tick(BooleanSupplier)} to call FC's
 * world-level tick logic ({@code World.ModSpecificTick()}).
 *
 * <p>FC's {@code ModSpecificTick()} handles per-tick world operations
 * such as custom block updates, weather effects, and other mod-specific
 * world processing that runs once per server tick per dimension.</p>
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    /**
     * Runs FC's {@code ModSpecificTick()} at the end of each server
     * level tick, after vanilla has finished all its own tick work.
     *
     * @param hasTimeLeft the vanilla time-budget supplier
     * @param ci          mixin callback info
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void btw$modSpecificTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel self = (ServerLevel) (Object) this;
        WorldBridge world = WorldBridge.getOrCreate(self);
        // FC's per-tick world logic (block updates, weather effects, etc.)
        world.ModSpecificTick();
    }
}
