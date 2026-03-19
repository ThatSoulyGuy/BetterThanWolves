package btw.forge.mixin;

import btw.forge.PlayerBridge;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link ServerPlayerGameMode#destroyBlock(BlockPos)} at HEAD,
 * BEFORE vanilla removes the block from the world. This is critical because
 * FC's {@code tryHarvestBlock} queries the world for the block at (x,y,z) —
 * if vanilla already removed it, the query returns air and nothing happens.
 *
 * FC's complete harvest pipeline runs here:
 * <ol>
 *   <li>{@code canHarvestBlock} — FC decides if tool is correct</li>
 *   <li>{@code CanConvertBlock} — FC checks if block transforms (log → damaged log)</li>
 *   <li>{@code ConvertBlock} — FC does the transformation, drops bark</li>
 *   <li>{@code harvestBlock} — proper tool drops</li>
 *   <li>{@code OnBlockDestroyedWithImproperTool} — component drops (piles, sawdust)</li>
 * </ol>
 *
 * Zero FC logic in this mixin — it just calls FC's {@code tryHarvestBlock}.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow protected ServerLevel level;
    @Shadow protected ServerPlayer player;

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("BTW-GameMode");

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void btw$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        LOGGER.info("[BTW-DEBUG] destroyBlock at {} state={}", pos, state);
        btw.modern.Block fcBlock = ProxyRegistry.getFcBlock(state.getBlock());
        LOGGER.info("[BTW-DEBUG] fcBlock={}", fcBlock != null ? fcBlock.getClass().getSimpleName() : "null");
        if (fcBlock == null) return; // Not an FC block — let vanilla handle it

        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(player);
        fcPlayer.syncFromReal();

        // FC's complete harvest pipeline — block is still in the world at this point
        btw.modern.ItemInWorldManager mgr = new btw.modern.ItemInWorldManager(
                WorldBridge.getOrCreate(level));
        mgr.thisPlayerMP = fcPlayer;
        mgr.tryHarvestBlock(pos.getX(), pos.getY(), pos.getZ(), 1);

        // Tell vanilla we handled it — don't let vanilla do its own removal/drops
        cir.setReturnValue(true);
    }
}
