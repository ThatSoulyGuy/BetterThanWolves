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
public class ProxyItem extends Item {
    private final int legacyId;
    private final String displayName;

    public ProxyItem(Properties props, int legacyId, String displayName) {
        super(props);
        this.legacyId = legacyId;
        this.displayName = displayName;
    }

    private btw.modern.Item fc() {
        btw.modern.Item item = btw.modern.Item.itemsList[legacyId];
        return item;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(displayName);
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
        if (fc() == null) return super.useOn(ctx);
        if (ctx.getLevel() instanceof ServerLevel sl) {
            PlayerBridge pb = PlayerBridge.getOrCreate(ctx.getPlayer());
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                boolean result = fc().onItemUse(fcStack, pb, WorldBridge.getOrCreate(sl),
                        ctx.getClickedPos().getX(), ctx.getClickedPos().getY(), ctx.getClickedPos().getZ(),
                        ctx.getClickedFace().get3DDataValue(),
                        (float) (ctx.getClickLocation().x - ctx.getClickedPos().getX()),
                        (float) (ctx.getClickLocation().y - ctx.getClickedPos().getY()),
                        (float) (ctx.getClickLocation().z - ctx.getClickedPos().getZ()));
                if (result) return InteractionResult.SUCCESS;
            }
        }
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
                fc().onEaten(fcStack, WorldBridge.getOrCreate(sl), pb);
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
