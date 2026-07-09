package btw.forge;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;

/**
 * A Forge Item that delegates ALL interactions to its corresponding FC item.
 * Every MC item callback is forwarded to the FC item's method via the
 * btw.modern layer — zero FC logic in this class, pure delegation.
 *
 * Used by BTWRegistration for FC items (IDs 256+) so that FC item subclasses
 * (FCItemAxe, FCItemFood, FCItemBow, etc.) have their full behavior expressed
 * through Forge's item system.
 */
public class ProxyItem extends Item implements LegacyProxyItem {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("BTW-ProxyItem");
    private final int legacyId;
    private final String displayName;

    public ProxyItem(Properties props, int legacyId, String displayName) {
        super(props);
        this.legacyId = legacyId;
        this.displayName = displayName;
    }

    @Override
    public int getLegacyId() {
        return legacyId;
    }

    private btw.modern.Item fc() {
        btw.modern.Item item = btw.modern.Item.itemsList[legacyId];
        return item;
    }

    @Override
    public Component getName(ItemStack stack) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                String name = fcItem.getItemDisplayName(fcStack);
                if (name != null && !name.isEmpty()) {
                    return Component.literal(name);
                }
            }
        }
        return Component.literal(displayName);
    }

    // ================================================================
    // Enchantment glint → FC hasEffect
    // ================================================================

    @Override
    public boolean isFoil(ItemStack stack) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                return fcItem.hasEffect(fcStack);
            }
        }
        return super.isFoil(stack);
    }

    // ================================================================
    // Tooltip lore → FC addInformation
    // ================================================================

    @Override
    public void appendHoverText(ItemStack stack, Level level, java.util.List<Component> tooltip, TooltipFlag flag) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                java.util.List<String> fcTooltip = new java.util.ArrayList<>();
                fcItem.addInformation(fcStack, null, fcTooltip, flag.isAdvanced());
                for (String line : fcTooltip) {
                    if (line != null && !line.isEmpty()) {
                        tooltip.add(Component.literal(line));
                    }
                }
            }
        }
    }

    // ================================================================
    // Right-click in air → FC onItemRightClick
    // ================================================================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (fc() == null) return super.use(level, player, hand);
        if (level instanceof ServerLevel sl) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                btw.modern.ItemStack result = fc().onItemRightClick(
                        fcStack, WorldBridge.getOrCreate(sl), pb);
                // FC may modify the stack or return a new one
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }

    // ================================================================
    // Right-click block → FC onItemUse (also handled by BlockBehaviorMixin)
    // ================================================================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // Handled by ServerPlayerGameModeMixin.btw$useItemOn which intercepts
        // at HEAD, calls FC's onItemUse, syncs inventory, and cancels the
        // original method (preventing creative-mode count restoration).
        return super.useOn(ctx);
    }

    // ================================================================
    // Finish eating/drinking/using → FC onEaten
    // ================================================================

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (fc() == null) return super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel sl && entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                // FC's onEaten applies hunger/saturation for ItemFood (and any
                // FC subclass override of onEaten). Nutrition lives in the FC
                // layer now — no bridge-side fixup required.
                btw.modern.ItemStack result = fc().onEaten(fcStack, WorldBridge.getOrCreate(sl), pb);

                pb.syncToReal();

                // Consume the item (decrease stack size) like vanilla does
                if (result != null && result != fcStack) {
                    // FC returned a different stack (e.g., bowl from stew)
                    // handled by FC's own logic
                }
                stack.shrink(1);
            }
        }
        return stack;
    }

    // ================================================================
    // Release bow / stop using → FC onPlayerStoppedUsing
    // ================================================================

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (fc() == null) { super.releaseUsing(stack, level, entity, timeLeft); return; }
        if (level instanceof ServerLevel sl && entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                fc().onPlayerStoppedUsing(fcStack, WorldBridge.getOrCreate(sl), pb, timeLeft);
            }
        }
    }

    // ================================================================
    // Use animation → FC getItemUseAction
    // ================================================================

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (fc() == null) return super.getUseAnimation(stack);
        btw.modern.EnumAction action = fc().getItemUseAction(ItemStackHelper.toFcStack(stack));
        if (action == null) return UseAnim.NONE;
        return switch (action.name()) {
            case "eat" -> UseAnim.EAT;
            case "drink" -> UseAnim.DRINK;
            case "bow" -> UseAnim.BOW;
            case "block" -> UseAnim.BLOCK;
            default -> UseAnim.NONE;
        };
    }

    // ================================================================
    // Use duration → FC getMaxItemUseDuration
    // ================================================================

    @Override
    public int getUseDuration(ItemStack stack) {
        if (fc() == null) return super.getUseDuration(stack);
        return fc().getMaxItemUseDuration(ItemStackHelper.toFcStack(stack));
    }

    // ================================================================
    // Per-tick inventory update → FC onUpdate
    // ================================================================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (fc() == null) return;
        if (level instanceof ServerLevel sl && entity instanceof Player player) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                fc().onUpdate(fcStack, WorldBridge.getOrCreate(sl), pb, slot, selected);
            }
        }
    }

    // ================================================================
    // Hit entity → FC hitEntity
    // ================================================================

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (fc() == null) return super.hurtEnemy(stack, target, attacker);
        // 1.5.2 flow damages the held stack in place (FCItemTool.hitEntity →
        // stack.damageItem(2, attacker)); route through PlayerBridge's live
        // inventory stack + writeback so the durability loss isn't discarded
        // on a throwaway toFcStack copy (same pattern as finishUsingItem).
        if (attacker instanceof Player player && !player.level().isClientSide()) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
                boolean result = fc().hitEntity(fcStack, fcTarget, pb);
                pb.syncInventoryToReal();
                return result;
            }
        }
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
        if (fcStack != null) {
            btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
            btw.modern.EntityLiving fcAttacker = LivingEntityBridge.wrapLiving(attacker);
            return fc().hitEntity(fcStack, fcTarget, fcAttacker);
        }
        return false;
    }

    // ================================================================
    // Mine block → FC onBlockDestroyed
    // ================================================================

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (fc() == null) return super.mineBlock(stack, level, state, pos, entity);
        if (level instanceof ServerLevel sl) {
            int blockId = ProxyRegistry.getBlockId(state.getBlock());
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                btw.modern.EntityLiving fcEntity = LivingEntityBridge.wrapLiving(entity);
                return fc().onBlockDestroyed(fcStack, WorldBridge.getOrCreate(sl),
                        blockId, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
            }
        }
        return false;
    }

    // ================================================================
    // Interact with entity → FC useItemOnEntity
    // ================================================================

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (fc() == null) return super.interactLivingEntity(stack, player, target, hand);
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
        if (fcStack != null) {
            btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
            boolean result = fc().useItemOnEntity(fcStack, fcTarget);
            if (result) return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
