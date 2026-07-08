package btw.forge.mixin;

import btw.forge.PlayerBridge;
import btw.forge.LivingEntityBridge;
import btw.forge.LegacyProxyItem;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import btw.forge.ItemStackHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts ALL item interaction methods on {@link ItemStack} and delegates
 * to the corresponding FC item if one exists. This is the GENERAL bridge —
 * it works for ANY item the player holds, whether vanilla (with FC replacement)
 * or BTW-specific (ProxyItem).
 *
 * For ProxyItems, the delegation happens twice (ProxyItem + this mixin) but
 * that's harmless since FC methods are idempotent for these calls.
 * For vanilla items with FC replacements, this mixin is the ONLY path to FC code.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * Look up the FC item for this MC ItemStack.
     */
    private btw.modern.Item btw$getFcItem() {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty()) return null;
        int legacyId;
        if (self.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = ProxyRegistry.getItemId(self.getItem());
        }
        if (legacyId > 0 && legacyId < btw.modern.Item.itemsList.length) {
            return btw.modern.Item.itemsList[legacyId];
        }
        return null;
    }

    // ================================================================
    // finishUsingItem → FC onEaten
    // ================================================================

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void btw$finishUsing(Level level, LivingEntity entity,
                                  CallbackInfoReturnable<ItemStack> cir) {
        // ProxyItems run onEaten in ProxyItem.finishUsingItem. Doing it here too
        // would double-apply nutrition now that ItemFood.onEaten is no longer a
        // no-op. This path handles only vanilla items with FC replacements.
        if (((ItemStack) (Object) this).getItem() instanceof LegacyProxyItem) return;
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        if (level instanceof ServerLevel sl && entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                fcItem.onEaten(fcStack, WorldBridge.getOrCreate(sl), pb);
                pb.syncToReal();
            }
        }
    }

    // ================================================================
    // releaseUsing → FC onPlayerStoppedUsing
    // ================================================================

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void btw$releaseUsing(Level level, LivingEntity entity, int timeLeft,
                                   CallbackInfo ci) {
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        if (level instanceof ServerLevel sl && entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                fcItem.onPlayerStoppedUsing(fcStack, WorldBridge.getOrCreate(sl), pb, timeLeft);
            }
        }
    }

    // ================================================================
    // hurtEnemy → FC hitEntity
    // ================================================================

    @Inject(method = "hurtEnemy", at = @At("HEAD"))
    private void btw$hurtEnemy(LivingEntity target, Player player, CallbackInfo ci) {
        // ProxyItems wrap target/attacker in ProxyItem.hurtEnemy; skip here to
        // avoid a second hitEntity call (e.g. double tool-durability loss on FC
        // weapons like the battleaxe). This path is for vanilla-item replacements.
        if (((ItemStack) (Object) this).getItem() instanceof LegacyProxyItem) return;
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack((ItemStack) (Object) this);
        if (fcStack != null) {
            btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
            btw.modern.EntityLiving fcAttacker = LivingEntityBridge.wrapLiving(player);
            fcItem.hitEntity(fcStack, fcTarget, fcAttacker);
        }
    }

    // ================================================================
    // mineBlock → FC onBlockDestroyed
    // ================================================================

    @Inject(method = "mineBlock", at = @At("HEAD"))
    private void btw$mineBlock(Level level, net.minecraft.world.level.block.state.BlockState state,
                                net.minecraft.core.BlockPos pos, Player player, CallbackInfo ci) {
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        if (level instanceof ServerLevel sl) {
            int blockId = ProxyRegistry.getBlockId(state.getBlock());
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack((ItemStack) (Object) this);
            if (fcStack != null) {
                PlayerBridge pb = PlayerBridge.getOrCreate(player);
                fcItem.onBlockDestroyed(fcStack, WorldBridge.getOrCreate(sl),
                        blockId, pos.getX(), pos.getY(), pos.getZ(), pb);
            }
        }
    }

    // ================================================================
    // getUseDuration → FC getMaxItemUseDuration
    // ================================================================

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void btw$getUseDuration(CallbackInfoReturnable<Integer> cir) {
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack((ItemStack) (Object) this);
        if (fcStack != null) {
            int duration = fcItem.getMaxItemUseDuration(fcStack);
            if (duration > 0) {
                cir.setReturnValue(duration);
            }
        }
    }

    // ================================================================
    // getUseAnimation → FC getItemUseAction
    // ================================================================

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void btw$getUseAnimation(CallbackInfoReturnable<net.minecraft.world.item.UseAnim> cir) {
        btw.modern.Item fcItem = btw$getFcItem();
        if (fcItem == null) return;
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack((ItemStack) (Object) this);
        if (fcStack != null) {
            btw.modern.EnumAction action = fcItem.getItemUseAction(fcStack);
            if (action != null && action != btw.modern.EnumAction.none) {
                net.minecraft.world.item.UseAnim anim = switch (action) {
                    case eat -> net.minecraft.world.item.UseAnim.EAT;
                    case drink -> net.minecraft.world.item.UseAnim.DRINK;
                    case bow -> net.minecraft.world.item.UseAnim.BOW;
                    case block -> net.minecraft.world.item.UseAnim.BLOCK;
                    default -> net.minecraft.world.item.UseAnim.NONE;
                };
                cir.setReturnValue(anim);
            }
        }
    }
}
