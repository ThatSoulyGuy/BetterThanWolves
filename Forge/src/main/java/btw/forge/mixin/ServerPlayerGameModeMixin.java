package btw.forge.mixin;

import btw.forge.InventoryBridge;
import btw.forge.ItemStackHelper;
import btw.forge.PlayerBridge;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

        // Sync FC item state (damage, stackSize) back to MC inventory.
        // mgr.lastHeldStack is the FC ItemStack that was modified inside
        // tryHarvestBlock (damageItem, stackSize changes).
        if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
            invBridge.writeBackCurrentItem(mgr.lastHeldStack);
        }

        // Tell vanilla we handled it — don't let vanilla do its own removal/drops
        cir.setReturnValue(true);
    }

    // ================================================================
    // useItemOn → FC Item.onItemUse (right-click on block)
    // Catches vanilla items with FC replacements when used on non-ProxyBlocks.
    // ProxyBlock.use() handles this for ProxyBlocks, but vanilla blocks don't
    // trigger ProxyBlock.use().
    // ================================================================

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void btw$useItemOn(ServerPlayer serverPlayer, Level world,
                                ItemStack mcStack, InteractionHand hand,
                                net.minecraft.world.phys.BlockHitResult hitResult,
                                CallbackInfoReturnable<InteractionResult> cir) {
        if (mcStack.isEmpty() || !(world instanceof net.minecraft.server.level.ServerLevel sl)) return;

        // Skip ProxyItems — they handle this in ProxyItem.useOn() or ProxyBlock.use()
        if (mcStack.getItem() instanceof btw.forge.ProxyItem) return;

        // Skip ProxyBlocks — ProxyBlock.use() handles both stages
        net.minecraft.core.BlockPos pos = hitResult.getBlockPos();
        if (world.getBlockState(pos).getBlock() instanceof btw.forge.ProxyBlock) return;

        // Look up FC item replacement
        int legacyId = ProxyRegistry.getItemId(mcStack.getItem());
        if (mcStack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        }
        btw.modern.Item fcItem = null;
        if (legacyId > 0 && legacyId < btw.modern.Item.itemsList.length) {
            fcItem = btw.modern.Item.itemsList[legacyId];
        }
        if (fcItem == null) return;

        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
        fcPlayer.syncFromReal();
        btw.modern.ItemStack fcStack = fcPlayer.getCurrentEquippedItem();
        if (fcStack == null) return;

        int side = hitResult.getDirection().get3DDataValue();
        net.minecraft.world.phys.Vec3 hitLoc = hitResult.getLocation();
        float hitX = (float)(hitLoc.x - pos.getX());
        float hitY = (float)(hitLoc.y - pos.getY());
        float hitZ = (float)(hitLoc.z - pos.getZ());

        boolean result = fcItem.onItemUse(fcStack, fcPlayer,
                WorldBridge.getOrCreate(sl),
                pos.getX(), pos.getY(), pos.getZ(), side, hitX, hitY, hitZ);

        if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
            invBridge.writeBackCurrentItem(fcStack);
        }

        if (result) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    // ================================================================
    // useItem → FC Item.onItemRightClick (right-click in air)
    // This catches ALL items — vanilla items with FC replacements AND BTW items.
    // ================================================================

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void btw$useItem(ServerPlayer serverPlayer, Level world,
                              ItemStack mcStack, InteractionHand hand,
                              CallbackInfoReturnable<InteractionResult> cir) {
        if (mcStack.isEmpty()) return;

        // Look up the FC item for whatever the player is holding
        int legacyId;
        if (mcStack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = ProxyRegistry.getItemId(mcStack.getItem());
        }

        btw.modern.Item fcItem = null;
        if (legacyId > 0 && legacyId < btw.modern.Item.itemsList.length) {
            fcItem = btw.modern.Item.itemsList[legacyId];
        }
        if (fcItem == null) return; // No FC item — let vanilla handle it

        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
        fcPlayer.syncFromReal();
        btw.modern.World fcWorld = WorldBridge.getOrCreate(level);

        btw.modern.ItemStack fcStack = fcPlayer.getCurrentEquippedItem();
        if (fcStack == null) return;

        // Call FC's onItemRightClick — FC decides what happens
        btw.modern.ItemStack result = fcItem.onItemRightClick(fcStack, fcWorld, fcPlayer);

        // If FC returned a different stack (consumed item, created new item), update
        if (result != fcStack && result != null) {
            // FC changed the held item — sync back
            // The FC code may have modified stack size, damage, etc.
        }

        // Check if FC's item has a use duration (food, bow, etc.)
        // If so, tell MC to start the "using item" animation
        int useDuration = fcItem.getMaxItemUseDuration(fcStack);
        if (useDuration > 0) {
            // FC item wants a use duration — let vanilla handle the use() call
            // which will start the using animation and eventually call
            // finishUsingItem/releaseUsing. ProxyItem handles those for FC items.
            // For vanilla items with FC replacements, we need to NOT cancel here.
            return;
        }

        // For instant-use items (no duration), FC already handled the interaction
        // Don't cancel — let vanilla also process if FC didn't consume
    }
}
