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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.Set;

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

    /** "hook:legacyId" keys already logged: an FC failure is reported once per item and hook. */
    private static final java.util.Set<String> FC_FAILURES_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Logs an exception thrown by FC item code, once per FC item id and hook (with stack trace).
     * Item callbacks run inside packet handlers (PacketUtils rethrows, crashing the integrated
     * server) and inventory/tick paths, so FC errors are caught and reported here instead.
     * Also used by ItemStackMixin for vanilla items with FC replacements.
     */
    public static void logFcFailureOnce(String hook, int fcItemId, Throwable t) {
        if (FC_FAILURES_LOGGED.add(hook + ":" + fcItemId)) {
            LOGGER.warn("FC item {} {} failed (further failures for this item and hook are not logged): {}",
                    fcItemId, hook, t.toString(), t);
        }
    }

    /**
     * True when {@code legacyBlockId} names a populated FC block slot. ProxyRegistry.getBlockId
     * returns 0 for modern-only blocks (deepslate, tuff, granite, ...), and 1.5.2 item code
     * dereferences {@code Block.blocksList[iBlockID]} unguarded -- FCItemTool.onBlockDestroyed
     * (Client FCItemTool.java:66), FCItemClub.onBlockDestroyed (Client FCItemClub.java:58);
     * FCItemAxe (Common FCItemAxe.java:123) null-checks but then calls the FCItemTool super -- so
     * passing such an id NPE'd on blocksList[0] whenever a survival player broke a
     * modern-only block with an FC or FC-replaced tool. The NPE escaped
     * ServerPlayerGameMode.destroyBlock into the packet handler, which rethrows: integrated-server
     * crash. Also used by ItemStackMixin.
     */
    public static boolean isFcBlockId(int legacyBlockId) {
        return legacyBlockId > 0 && legacyBlockId < btw.modern.Block.blocksList.length
                && btw.modern.Block.blocksList[legacyBlockId] != null;
    }

    /**
     * Legacy block id to hand FC onBlockDestroyed for a block a tool just mined (mineBlock runs before
     * ServerPlayerGameMode.destroyBlock removes it, :250/:253), or 0 to skip FC. The world-read id
     * (ProxyRegistry.getWorldBlockId, design_blockmap cross-cluster 1): a modern-only block reads as a
     * ModernBlockStandIn whose getBlockHardness is the MC destroy speed, so FC's tool wear is right; never an
     * id without a block (see isFcBlockId). A liquid id is skipped: getWorldBlockId reads seagrass/kelp as
     * water 8 (triage D5), and FCItemTool would wear the tool on water's hardness 100 for an instant break.
     */
    public static int minedBlockIdForFc(BlockState state, ServerLevel level, BlockPos pos) {
        int blockId = ProxyRegistry.getWorldBlockId(state, level, pos);
        if (!isFcBlockId(blockId)) return 0;
        btw.modern.Material material = btw.modern.Block.blocksList[blockId].blockMaterial;
        return material != null && material.isLiquid() ? 0 : blockId;
    }

    /**
     * The FC item that handles a stack of {@code item}, or null when FC has none: its 1.5.2 item id first
     * (ProxyRegistry.getItemId: ProxyItems, the vanilla item table, flattened subtypes), and only for a
     * BlockItem without an FC item there, its block's ItemBlock. 1.20.1 made carrots, potatoes, seeds,
     * string, redstone, sugar cane and the cauldron/brewing stand/sign/door/bed/skull items BlockItems of
     * their crop/wire/block, where 1.5.2 kept separate item ids (carrot 391 vs the carrot crop 141, seeds
     * 295 vs crops 59; vanilla/client Item.java). Resolving every BlockItem through its block reached the
     * crop's ItemBlock instead of the FC food/seed item, so e.g. eaten carrots ran ItemBlock.onEaten
     * (nothing) while FoodDataMixin had already cancelled vanilla's nutrition. Same order as
     * ProxyRegistry.getOrCreateLegacyId (design_blockmap cross-cluster). Used by ItemStackMixin and
     * ServerPlayerGameModeMixin.
     */
    public static btw.modern.Item fcItemFor(Item item) {
        if (item == null) return null;
        btw.modern.Item fcItem = fcItemAt(ProxyRegistry.getItemId(item));
        if (fcItem == null && item instanceof net.minecraft.world.item.BlockItem bi) {
            fcItem = fcItemAt(ProxyRegistry.getBlockId(bi.getBlock()));
        }
        return fcItem;
    }

    private static btw.modern.Item fcItemAt(int legacyId) {
        return legacyId > 0 && legacyId < btw.modern.Item.itemsList.length
                ? btw.modern.Item.itemsList[legacyId] : null;
    }

    /**
     * The FC entity FC item code should see for {@code target}: an FC puppet's own FC entity (so
     * checks like {@code entity instanceof FCEntitySheep} in FCItemDye/FCItemBreedingHarness
     * match), otherwise the generic bridge (PlayerBridge for players).
     */
    private static btw.modern.EntityLiving fcLivingFor(LivingEntity target) {
        btw.modern.EntityLiving fc = null;
        if (target instanceof ProxyMob pm) fc = pm.getFcEntity();
        else if (target instanceof ProxyAnimal pa) fc = pa.getFcEntity();
        else if (target instanceof ProxyPathfinderMob pp) fc = pp.getFcEntity();
        return fc != null ? fc : LivingEntityBridge.wrapLiving(target);
    }

    /**
     * Per FC item class: does it override either 1.5.2 entity-interaction hook? Only Client
     * FCItemDye, FCItemDung and FCItemBreedingHarness do; Modern-Common Item returns false for
     * both, so other items skip the stack conversion and entity wrapping.
     */
    private static final ClassValue<Boolean> ENTITY_INTERACTION_OVERRIDDEN = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return declaredBelowItem(type, "itemInteractionForEntity") || declaredBelowItem(type, "useItemOnEntity");
        }
    };

    private static boolean declaredBelowItem(Class<?> type, String name) {
        try {
            return type.getMethod(name, btw.modern.ItemStack.class, btw.modern.EntityLiving.class)
                    .getDeclaringClass() != btw.modern.Item.class;
        } catch (Throwable t) {
            return true;   // cannot tell: call it (the base implementations return false)
        }
    }

    /**
     * Runs FC's entity interaction for the item in {@code stack} on {@code target}, as 1.5.2
     * EntityPlayer.interactWith -> ItemStack.interactWith did once the entity itself declined
     * (vanilla/client EntityPlayer.java:1182-1206, ItemStack.java:416-419). 1.20.1
     * Player.interactOn calls interactLivingEntity at the same point, with the live hand stack
     * (a copy in creative, like 1.5.2). The shipped FC item classes are the Client variants, which
     * override the 1.5.2 CLIENT name itemInteractionForEntity; the Server variants used
     * useItemOnEntity (vanilla/server Item.java:607, ItemStack.java:394), so both are tried.
     * FC edits the stack in place (FCItemDye.java:64, FCItemBreedingHarness.java:77
     * {@code stackSize--}); that count is applied to {@code stack}. Server only. Never throws.
     *
     * @return true if FC handled the interaction
     */
    public static boolean interactWithEntityFc(ItemStack stack, LivingEntity target) {
        int fcItemId = 0;
        try {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack == null || fcStack.itemID <= 0 || fcStack.itemID >= btw.modern.Item.itemsList.length) return false;
            fcItemId = fcStack.itemID;
            btw.modern.Item fcItem = btw.modern.Item.itemsList[fcItemId];
            if (fcItem == null || !ENTITY_INTERACTION_OVERRIDDEN.get(fcItem.getClass())) return false;
            int before = fcStack.stackSize;
            btw.modern.EntityLiving fcTarget = fcLivingFor(target);
            if (!fcItem.itemInteractionForEntity(fcStack, fcTarget) && !fcItem.useItemOnEntity(fcStack, fcTarget)) {
                return false;
            }
            if (fcStack.stackSize != before) stack.setCount(Math.max(fcStack.stackSize, 0));
            return true;
        } catch (Throwable t) {
            logFcFailureOnce("itemInteractionForEntity", fcItemId, t);
            return false;
        }
    }

    // ================================================================
    // Taken from a crafting result → FC onCreated (audit sounds S9)
    // ================================================================

    /**
     * Per FC item class: does it override 1.5.2 Item.onCreated? Thirteen FC items do, all for their crafting
     * sound (e.g. Client FCItemTool.java:124-143, Common FCItemArmor.java:39-54); Modern-Common Item.onCreated is
     * empty, so every other item skips the stack conversion and the player bridging.
     */
    private static final ClassValue<Boolean> ON_CREATED_OVERRIDDEN = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("onCreated", btw.modern.ItemStack.class, btw.modern.World.class,
                        btw.modern.EntityPlayer.class).getDeclaringClass() != btw.modern.Item.class;
            } catch (Throwable t) {
                return true;   // cannot tell: call it (the base implementation is empty)
            }
        }
    };

    /**
     * 1.5.2 SlotCrafting/SlotFurnace.onCrafting -> ItemStack.onCrafting -> Item.onCreated (vanilla/client
     * SlotCrafting.java:72-75, ItemStack.java:501-504), which 1.20.1 has as ItemStack.onCraftedBy -> Item.onCraftedBy
     * (ResultSlot/FurnaceResultSlot.checkTakeAchievements, InventoryMenu/CraftingMenu.quickMoveStack), on both sides.
     */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        runFcOnCreated(fc(), stack, level, player);
    }

    /**
     * Runs FC's Item.onCreated for a stack {@code player} just took from a crafting (or smelting) result, on the
     * side this runs on, as 1.5.2 ran it on both. FC plays its crafting sounds only on the client
     * ({@code world.isRemote}, once per tick through m_iTimesCraftedThisTick) through the client player's
     * playSound, which plays locally (vanilla/client EntityPlayerSP.java:663-666): the client gets a client FC
     * world and {@link ClientCrafter}; on the server FC's isRemote guard makes the call a no-op, kept for
     * parity. Used by {@link #onCraftedBy} and by ItemStackMixin for vanilla items with an FC replacement (the
     * vanilla tools, swords, armour, shears), and by FCContainerMenu.ClientCraftResultSlot for FC containers
     * (FC workbench, soulforge), whose server SlotCrafting already calls onCreated. Never throws.
     */
    public static void runFcOnCreated(ItemStack stack, Level level, Player player) {
        if (stack == null || stack.isEmpty()) return;
        runFcOnCreated(fcItemFor(stack.getItem()), stack, level, player);
    }

    private static void runFcOnCreated(btw.modern.Item fcItem, ItemStack stack, Level level, Player player) {
        if (fcItem == null || stack == null || stack.isEmpty() || level == null || player == null) return;
        if (!ON_CREATED_OVERRIDDEN.get(fcItem.getClass())) return;
        try {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack == null) return;
            if (level instanceof ServerLevel sl) {
                if (player instanceof ServerPlayer sp) {
                    fcItem.onCreated(fcStack, WorldBridge.getOrCreate(sl), PlayerBridge.getOrCreate(sp));
                }
            } else if (level.isClientSide()) {
                ClientCrafter.onCreated(fcItem, fcStack, level, player);
            }
        } catch (Throwable t) {
            // ResultSlot.onTake runs inside the container click handler (PacketUtils rethrows on the server)
            logFcFailureOnce("onCreated", fcItem.itemID, t);
        }
    }

    /**
     * The 1.5.2 client player FC's client-side onCreated sees: m_iTimesCraftedThisTick counts the crafts of the
     * current client tick (1.5.2 zeroed it in EntityPlayer.onUpdate, vanilla/client EntityPlayer.java:227, and
     * SlotCrafting counted each craft, SlotCrafting.java:184), and playSound plays at the player's feet on this
     * client only (EntityPlayerSP.playSound, EntityPlayerSP.java:663-666) through the shared client sink
     * (LegacyClientSound.playLocal). Built only for client levels, on the render thread; holds its player weakly
     * and no world between calls, so a closed world is never kept reachable.
     */
    private static final class ClientCrafter extends btw.modern.EntityClientPlayerMP {
        /** The local player's crafter (render thread only). */
        private static ClientCrafter current;

        private final java.lang.ref.WeakReference<Player> owner;
        private int craftTick;

        private ClientCrafter(Player player) {
            super((btw.modern.World) null);   // as PlayerBridge: built without a world, given one per call
            this.owner = new java.lang.ref.WeakReference<>(player);
            this.craftTick = player.tickCount;
        }

        static void onCreated(btw.modern.Item fcItem, btw.modern.ItemStack fcStack, Level level, Player player) {
            ClientCrafter crafter = current;
            if (crafter == null || crafter.owner.get() != player) {
                crafter = new ClientCrafter(player);
                current = crafter;
            }
            if (crafter.craftTick != player.tickCount) {
                crafter.craftTick = player.tickCount;
                crafter.m_iTimesCraftedThisTick = 0;
            }
            crafter.posX = player.getX();
            crafter.posY = player.getY();
            crafter.posZ = player.getZ();
            btw.modern.World world = ProxyEntity.createDummyClientWorld(level);
            crafter.worldObj = world;
            try {
                fcItem.onCreated(fcStack, world, crafter);
            } finally {
                crafter.worldObj = null;
                crafter.m_iTimesCraftedThisTick++;
            }
        }

        @Override
        public void playSound(String name, float volume, float pitch) {
            // Only ever built for a client level (runFcOnCreated), so the client-only sink is never reached
            // on a server.
            btw.forge.client.LegacyClientSound.playLocal(posX, posY, posZ, name, volume, pitch, false);
        }
    }

    // ================================================================
    // Vanilla eat sounds where FC's onEaten owns the eat (audit sounds S10)
    // ================================================================

    /**
     * Implemented on ItemStack by ItemStackMixin: true while finishUsingItem runs an eat that FC's 1.5.2
     * ItemFood.onEaten already fed (PlayerBridge.fcFedForEat), whose 'random.burp' FC plays itself
     * (vanilla/server ItemFood.java:71). PlayerMixin then drops Player.eat's PLAYER_BURP, and ItemStackMixin
     * makes LivingEntity.eat's closing eat sound (no 1.5.2 equivalent) {@link #SILENT_SOUND}.
     */
    public interface FcOwnedEat {
        boolean btw$isFcOwnedEat();
    }

    /**
     * A sound event that plays nothing: 1.20.1's client resolves minecraft:intentionally_empty to its silent
     * sound without a missing-sound warning (AbstractSoundInstance.resolve, SoundEngine.play).
     */
    public static final net.minecraft.sounds.SoundEvent SILENT_SOUND = net.minecraft.sounds.SoundEvent
            .createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("minecraft", "intentionally_empty"));

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
        // The FC player exposes only the main hand (InventoryBridge.getCurrentItem; 1.5.2 has no off
        // hand). An off-hand use would run this item's FC code on the main-hand stack, and the
        // write-back below would then consume or damage that unrelated stack.
        if (level instanceof ServerLevel sl && hand == InteractionHand.MAIN_HAND) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                btw.modern.ItemStack result = fcStack;
                try {
                    result = fc().onItemRightClick(fcStack, WorldBridge.getOrCreate(sl), pb);
                } catch (Throwable t) {
                    logFcFailureOnce("onItemRightClick", legacyId, t);
                }
                // 1.5.2 ItemInWorldManager.tryUseItem (vanilla/server ItemInWorldManager.java:338-361):
                // the returned stack becomes the held stack, cleared at stackSize 0 (a full FC bucket
                // returns its empty bucket). Edits FC makes in place -- FCItemThrowable's
                // stackSize-- (FCItemThrowable.java:33), FCItemDynamite's flint-and-steel damage --
                // are only in the PlayerBridge snapshot until written back, slot by slot.
                int slot = player.getInventory().selected;
                if (result != fcStack && slot >= 0 && slot < pb.inventory.mainInventory.length) {
                    pb.inventory.setInventorySlotContents(slot,
                            result != null && result.stackSize > 0 ? result : null);
                }
                pb.syncInventoryToReal();
                return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }

    // ================================================================
    // Right-click block → FC onItemUse (ProxyBlock.tryPlaceHeldItem)
    // ================================================================

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: FC onItemUse never runs here. A player's click runs it through
        // ProxyBlock.tryPlaceHeldItem: Stage 2 inside the clicked block's use (ProxyBlock.use,
        // BlockBehaviorMixin.btw$use), or ServerPlayerGameModeMixin.btw$fcItemStep, which runs just before this call in
        // ServerPlayerGameMode.useItemOn and cancels it when FC acted (its own creative restore replacing 1.20.1's). Doing
        // it here would run FC under Forge's block-snapshot capture (ForgeHooks.onPlaceItemIntoWorld), which defers
        // onPlace. A caller that reaches this directly (a deployer-style fake player) gets 1.20.1's default.
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
            // The FC player holds only the main hand (InventoryBridge.getCurrentItem; 1.5.2 has no off hand): the
            // selected main-hand stack runs on its live snapshot, any other stack on a detached copy. FC's in-place
            // edits to the stack it is handed (FCItemBucketDrinkable's --stackSize) must never reach an unrelated
            // main-hand stack through the write-back below.
            btw.modern.ItemStack fcStack;
            try {
                fcStack = stack == player.getMainHandItem()
                        ? pb.getCurrentEquippedItem() : ItemStackHelper.toFcStack(stack);
            } catch (Throwable t) {
                logFcFailureOnce("onEaten", legacyId, t);
                return stack;
            }
            if (fcStack != null) {
                // FC's onEaten applies hunger/saturation for ItemFood (and any
                // FC subclass override of onEaten). Nutrition lives in the FC
                // layer now — no bridge-side fixup required.
                btw.modern.ItemStack result = fcStack;
                try {
                    result = fc().onEaten(fcStack, WorldBridge.getOrCreate(sl), pb);
                } catch (Throwable t) {
                    // Runs inside LivingEntity.updatingUsingItem: a throw would crash "Ticking player". The
                    // item is still eaten below: 1.5.2 decrements before the nutrition (vanilla/server
                    // ItemFood.java:69).
                    logFcFailureOnce("onEaten", legacyId, t);
                }

                pb.syncToReal();

                // Consume the item like 1.5.2 ItemFood.onEaten (--stackSize; Modern-Common's leaves it to the
                // bridge). Before the write-back: an FC onEaten that already decremented the held stack itself
                // (FCItemBucketDrinkable.java:66-69) then lands its own count over this one instead of a second
                // item being lost. Not in creative, where 1.20.1 keeps the used stack (LivingEntity.eat:3277,
                // MilkBucketItem.finishUsingItem:26-28) and FC's drinks do too (FCItemBucketDrinkable.java:66,
                // FCItemPotion.java:42): creative chocolate milk was used up. FC ItemFoods never get here in
                // creative: FC refuses the eat (Modern-Common EntityPlayer.canEat:88, disableDamage, as 1.5.2
                // vanilla/server EntityPlayer.java:1964), and a ProxyItem has no FoodProperties for vanilla's
                // Item.use to start the eat instead.
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // FC's inventory edits: FCItemSoup.onEaten (Common FCItemSoup.java:29-41: chicken soup, chowder,
                // hearty stew) returns the bowl with player.inventory.addItemStackToInventory, which only reaches
                // the InventoryBridge snapshot; the next sync() overwrote it and the bowl was lost. The write-back
                // is selective (InventoryBridge.writeBackAll), so only the slot FC put the bowl in is written: the
                // first bowl stack or empty slot, as in 1.5.2 (vanilla/server EntityPlayer.java:406-426 clears
                // the emptied held slot only after onEaten). A full inventory drops it (PlayerBridge.dropPlayerItem).
                try {
                    pb.syncInventoryToReal();
                } catch (Throwable t) {
                    logFcFailureOnce("onEaten write-back", legacyId, t);
                }
                // 1.5.2 EntityPlayer.onItemUseFinish (vanilla/server EntityPlayer.java:412-422) puts a different stack
                // onEaten returns into the held slot: FCItemBucketDrinkable's empty bucket once the last drink is gone
                // (Common FCItemBucketDrinkable.java:76, chocolate milk), the knitting needles, the carving bone's
                // hook. Dropping it lost the bucket. Returned here, LivingEntity.completeUsingItem (:3022-3025) sets
                // it in the hand used; the write-back above already emptied the main-hand slot FC drained. FC soups
                // return the eaten stack itself, so they keep returning `stack`.
                if (result != null && result != fcStack && result.stackSize > 0) {
                    try {
                        ItemStack replacement = ItemStackHelper.toMcStack(result);
                        if (!replacement.isEmpty()) return replacement;
                    } catch (Throwable t) {
                        logFcFailureOnce("onEaten result", legacyId, t);
                    }
                }
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
        // Main hand only: the FC player's held item is the main-hand stack (see use()).
        if (level instanceof ServerLevel sl && entity instanceof Player player
                && player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            PlayerBridge pb = PlayerBridge.getOrCreate(player);
            pb.syncFromReal();
            btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
            if (fcStack != null) {
                boolean hadBow = fcStack.stackSize > 0;
                try {
                    fc().onPlayerStoppedUsing(fcStack, WorldBridge.getOrCreate(sl), pb, timeLeft);
                } catch (Throwable t) {
                    logFcFailureOnce("onPlayerStoppedUsing", legacyId, t);
                }
                // A bow FC wore out (damageItem, FCItemBow.java:82): the break effect 1.5.2's
                // renderBrokenItemStack played (vanilla/server ItemStack.java:331), sent before the
                // write-back empties the slot, as 1.20.1 ItemStack.hurtAndBreak does (:365-367).
                if (hadBow && fcStack.stackSize <= 0) player.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                // FCItemBow consumes the arrow (InventoryPlayer.consumeInventoryItem,
                // FCItemBow.java:60) and damages the bow (:82) in place; without the write-back the
                // FC bows fired for free and never wore.
                pb.syncInventoryToReal();
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
    // Held use, every tick → FC UpdateUsingItem (knitting, bone carving, wicker weaving)
    // ================================================================

    /**
     * Per FC item class: does it override 1.5.2 Item.UpdateUsingItem? Only FCItemCraftingProgressive does
     * (FCItemCraftingProgressive.java:57-87; knitting, bone carving and wicker weaving inherit it). Modern-Common
     * Item.UpdateUsingItem is empty (Item.java:472), so foods, drinks, bows and block-action tools return at once.
     */
    private static final ClassValue<Boolean> USE_TICK_OVERRIDDEN = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("UpdateUsingItem", btw.modern.ItemStack.class, btw.modern.World.class,
                        btw.modern.EntityPlayer.class).getDeclaringClass() != btw.modern.Item.class;
            } catch (Throwable t) {
                return true;   // cannot tell: call it (the base implementation is empty)
            }
        }
    };

    /**
     * 1.5.2 EntityPlayer.onUpdate ran the held item's UpdateUsingItem every tick of a use, before the countdown, on both
     * sides (vanilla/server EntityPlayer.java:229 then :232; vanilla/client :252 then :255). 1.20.1 calls onUseTick at
     * the same point (LivingEntity.updateUsingItem :2898, decrement :2903), on both sides. After the warm-up the FC
     * server step takes 1 off the damage every 4 ticks and at 0 calls SetItemInUseCount(1), which PlayerBridge writes to
     * the real counter, so this tick's decrement completes the use (completeUsingItem -> finishUsingItem -> FC onEaten).
     * Main hand only, as use(). Server: the live main-hand snapshot, damage written back onto the same MC stack
     * (InventoryBridge.writeBackSlot), which completeUsingItem's identity test needs (:3016). Client: a detached copy on
     * the render thread's ClientFcWorld for FC's client-only effects (FCItemCarvingBone particles); FC edits stack and
     * counter only when !world.isRemote, and the client bridge's null-player sound is silent
     * (ClientLevel.playSeededSound :472), so the server broadcast stays the one sound.
     */
    // FCMOD-INTEGRATION(1.20.1): held_use: FC's per-tick held-use step runs, so knitting, bone carving and wicker weaving progress and finish as in 4.B
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        btw.modern.Item fcItem = fc();
        if (fcItem == null || !USE_TICK_OVERRIDDEN.get(fcItem.getClass())) return;
        if (!(entity instanceof Player player) || player.getUsedItemHand() != InteractionHand.MAIN_HAND
                || stack != player.getMainHandItem()) return;
        PlayerBridge serverBridge = null;
        try {
            if (level instanceof ServerLevel sl) {
                PlayerBridge pb = serverBridge = PlayerBridge.getOrCreate(player);   // syncs the inventory snapshot
                btw.modern.ItemStack fcStack = pb.getCurrentEquippedItem();
                if (fcStack == null || fcStack.itemID != legacyId) return;
                fcItem.UpdateUsingItem(fcStack, WorldBridge.getOrCreate(sl), pb);
                pb.syncInventoryToReal();
            } else if (level.isClientSide()) {
                btw.modern.World fcWorld = btw.forge.farming.FcSoil.fcWorld(level);
                btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
                if (fcWorld == null || fcStack == null) return;
                fcItem.UpdateUsingItem(fcStack, fcWorld, PlayerBridge.getOrCreate(player));
            }
        } catch (Throwable t) {
            // Runs inside LivingEntity.tick: a throw would crash the entity tick.
            logFcFailureOnce("UpdateUsingItem", legacyId, t);
            // INV-RESYNC: a failed FC call lands nothing; the next sync dropped its edits, it would now write them
            if (serverBridge != null) serverBridge.discardInventoryEdits();
        }
    }

    /**
     * 1.5.2 FC kept the held item from re-equipping when only its damage changed, for items whose
     * IgnoreDamageWhenComparingDuringUse is true (vanilla/client ItemRenderer.java:720-729). 1.20.1's default re-equips on
     * any new stack object (IForgeItem.shouldCauseReequipAnimation via ItemInHandRenderer.tick), so the hand would dip on
     * every progress step the server sends.
     */
    // FCMOD-INTEGRATION(1.20.1): held_use: a hold-to-craft item's progress steps do not re-equip the hand, as in 1.5.2
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null && !slotChanged && oldStack.getItem() == newStack.getItem()
                && fcItem.IgnoreDamageWhenComparingDuringUse()) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
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
                try {
                    fc().onUpdate(fcStack, WorldBridge.getOrCreate(sl), pb, slot, selected);
                } catch (Throwable t) {
                    // every tick for every FC item in the inventory, inside ServerPlayer.doTick
                    logFcFailureOnce("onUpdate", legacyId, t);
                }
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
                boolean result = false;
                try {
                    btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
                    result = fc().hitEntity(fcStack, fcTarget, pb);
                } catch (Throwable t) {
                    // Player.attack, inside the interact packet handler (PacketUtils rethrows Errors)
                    logFcFailureOnce("hitEntity", legacyId, t);
                }
                pb.syncInventoryToReal();   // wear FC applied before any throw
                return result;
            }
        }
        try {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            if (fcStack != null) {
                btw.modern.EntityLiving fcTarget = LivingEntityBridge.wrapLiving(target);
                btw.modern.EntityLiving fcAttacker = LivingEntityBridge.wrapLiving(attacker);
                return fc().hitEntity(fcStack, fcTarget, fcAttacker);
            }
        } catch (Throwable t) {
            logFcFailureOnce("hitEntity", legacyId, t);
        }
        return false;
    }

    // ================================================================
    // Mine block → FC onBlockDestroyed
    // ================================================================

    /**
     * FC's onBlockDestroyed on the player's live held FC stack, written back. 1.5.2 wears the HELD stack in place
     * (vanilla/server ItemInWorldManager.java:494-501 currentStack.onBlockDestroyed). As in hurtEnemy: the
     * PlayerBridge's live snapshot plus a write-back, so FC's wear is not discarded with a throwaway toFcStack copy (FC
     * tools took no durability from modern-only blocks). Null when {@code stack} is not the main-hand stack of a player
     * with a held FC stack. Shared by {@link #mineBlock} and ItemStackMixin.btw$mineBlock (vanilla_pair_drops).
     */
    public static Boolean onBlockDestroyedLive(btw.modern.Item fcItem, ItemStack stack, ServerLevel sl, int blockId,
                                               BlockPos pos, LivingEntity entity) {
        if (!(entity instanceof Player player) || player.getMainHandItem() != stack) return null;
        PlayerBridge pb = PlayerBridge.getOrCreate(player);
        btw.modern.ItemStack held = pb.getCurrentEquippedItem();
        if (held == null) return null;
        try {
            return fcItem.onBlockDestroyed(held, WorldBridge.getOrCreate(sl),
                    blockId, pos.getX(), pos.getY(), pos.getZ(), pb);
        } finally {
            boolean broke = held.stackSize <= 0;
            pb.syncInventoryToReal();   // wear (or a broken tool) FC applied before any throw
            if (broke) {
                // the break sound/particles MC's own hurtAndBreak would have sent (DiggerItem)
                player.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                // FCMOD-INTEGRATION(1.20.1): vanilla_pair_drops: the write-back emptied the slot; empty the caller's stack too, as hurtAndBreak's shrink does, so ServerPlayerGameMode.destroyBlock posts PlayerDestroyItemEvent (:251-252)
                if (player.getMainHandItem() != stack && !stack.isEmpty()) stack.setCount(0);
            }
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (fc() == null) return super.mineBlock(stack, level, state, pos, entity);
        if (level instanceof ServerLevel sl) {
            // Reached for blocks ServerPlayerGameModeMixin.btw$destroyBlock leaves to vanilla: no FC block
            // (deepslate, tuff, granite, ...), and the beds and doors of vanilla_pair_drops. FC gets their
            // world-read id (minedBlockIdForFc), so FCItemTool.onBlockDestroyed (Client FCItemTool.java:64-72)
            // wears the tool as 1.5.2 did for any block of hardness > 0.
            int blockId = minedBlockIdForFc(state, sl, pos);
            if (blockId == 0) return false;
            try {
                Boolean live = onBlockDestroyedLive(fc(), stack, sl, blockId, pos, entity);
                if (live != null) return live;
                btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
                if (fcStack != null) {
                    btw.modern.EntityLiving fcEntity = LivingEntityBridge.wrapLiving(entity);
                    return fc().onBlockDestroyed(fcStack, WorldBridge.getOrCreate(sl),
                            blockId, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
                }
            } catch (Throwable t) {
                logFcFailureOnce("onBlockDestroyed", legacyId, t);
            }
        }
        return false;
    }

    // ================================================================
    // FC tools are tools: ToolActions, efficiency and drops on 1.20.1-only blocks (INTEGRATIONS #022)
    // ================================================================

    /** What an FC item class is to 1.20.1: an FC tool (FCItemTool) and the DIG actions of its family. */
    private record FcToolKind(boolean tool, Set<ToolAction> digActions) {
        static final FcToolKind NONE = new FcToolKind(false, Set.of());
    }

    /**
     * The FC tool classes, loaded once without initialising them (the Forge source set cannot name FC classes);
     * null when one is missing. Mattock first: FCItemMattock extends FCItemRefinedPickAxe extends FCItemPickaxe, and
     * FC ORs the refined shovel into it (Common FCItemMattock.java:31-58). FCItemRefinedAxe and FCItemBattleAxe extend
     * FCItemAxe, FCItemRefinedShovel FCItemShovel, FCItemRefinedHoe FCItemHoe (javap).
     */
    private static final class FcToolClasses {
        static final Class<?> TOOL = load("net.minecraft.src.btw.item.FCItemTool");
        static final Class<?> MATTOCK = load("net.minecraft.src.btw.item.FCItemMattock");
        static final Class<?> AXE = load("net.minecraft.src.btw.item.FCItemAxe");
        static final Class<?> PICKAXE = load("net.minecraft.src.btw.item.FCItemPickaxe");
        static final Class<?> SHOVEL = load("net.minecraft.src.btw.item.FCItemShovel");
        static final Class<?> HOE = load("net.minecraft.src.btw.item.FCItemHoe");
        static final Class<?> CHISEL = load("net.minecraft.src.btw.item.FCItemChisel");
        /** FCItemTool.efficiencyOnProperMaterial (public float) and toolMaterial (public EnumToolMaterial). */
        static final java.lang.reflect.Field EFFICIENCY = field("efficiencyOnProperMaterial");
        static final java.lang.reflect.Field MATERIAL = field("toolMaterial");

        private static Class<?> load(String name) {
            try {
                return Class.forName(name, false, ProxyItem.class.getClassLoader());
            } catch (Throwable t) {
                LOGGER.warn("FC tool class {} not found; its items act as plain items to 1.20.1: {}",
                        name, t.toString());
                return null;
            }
        }

        private static java.lang.reflect.Field field(String name) {
            try {
                return TOOL != null ? TOOL.getField(name) : null;
            } catch (Throwable t) {
                LOGGER.warn("FCItemTool.{} not found: {}", name, t.toString());
                return null;
            }
        }

        private static boolean is(Class<?> family, Class<?> type) {
            return family != null && family.isAssignableFrom(type);
        }
    }

    /**
     * Per FC item class, resolved once (lazily, so after FC init): DIG actions only. Never AXE_STRIP, AXE_SCRAPE,
     * AXE_WAX_OFF, SHOVEL_FLATTEN or HOE_TILL: an FC tool's right click is FC onItemUse (tool placement), and Dynamic
     * Trees strips only with AXE_STRIP (BranchBlock.java:207). Chisels and the club dig no 1.20.1 tag.
     */
    private static final ClassValue<FcToolKind> FC_TOOL_KIND = new ClassValue<>() {
        @Override
        protected FcToolKind computeValue(Class<?> type) {
            if (!FcToolClasses.is(FcToolClasses.TOOL, type)) return FcToolKind.NONE;
            Set<ToolAction> actions;
            if (FcToolClasses.is(FcToolClasses.MATTOCK, type)) {
                actions = Set.of(ToolActions.PICKAXE_DIG,
                        ToolActions.SHOVEL_DIG);
            } else if (FcToolClasses.is(FcToolClasses.AXE, type)) {
                actions = Set.of(ToolActions.AXE_DIG);
            } else if (FcToolClasses.is(FcToolClasses.PICKAXE, type)) {
                actions = Set.of(ToolActions.PICKAXE_DIG);
            } else if (FcToolClasses.is(FcToolClasses.SHOVEL, type)) {
                actions = Set.of(ToolActions.SHOVEL_DIG);
            } else if (FcToolClasses.is(FcToolClasses.HOE, type)) {
                actions = Set.of(ToolActions.HOE_DIG);
            } else {
                actions = Set.of();
                if (!FcToolClasses.is(FcToolClasses.CHISEL, type)) {
                    LOGGER.info("FC tool class {} has no 1.20.1 dig family; it declares no ToolAction",
                            type.getName());
                }
            }
            return new FcToolKind(true, actions);
        }
    };

    private FcToolKind fcToolKind() {
        btw.modern.Item fcItem = fc();
        return fcItem != null ? FC_TOOL_KIND.get(fcItem.getClass()) : FcToolKind.NONE;
    }

    /** FC's efficiencyOnProperMaterial, read once (FCItemHoe's constructor already divided it); NaN until read. */
    private volatile float cachedFcEfficiency = Float.NaN;
    /** FC's toolMaterial harvest level, read once; Integer.MIN_VALUE until read, -1 if unreadable. */
    private volatile int cachedFcHarvestLevel = Integer.MIN_VALUE;

    private float fcEfficiency(btw.modern.Item fcItem) {
        float eff = cachedFcEfficiency;
        if (Float.isNaN(eff)) {
            try {
                eff = FcToolClasses.EFFICIENCY != null ? FcToolClasses.EFFICIENCY.getFloat(fcItem) : 0F;
            } catch (Throwable t) {
                logFcFailureOnce("efficiencyOnProperMaterial", legacyId, t);
                eff = 0F;
            }
            cachedFcEfficiency = eff;
        }
        return eff;
    }

    private int fcHarvestLevel(btw.modern.Item fcItem) {
        int level = cachedFcHarvestLevel;
        if (level == Integer.MIN_VALUE) {
            try {
                Object material = FcToolClasses.MATERIAL != null ? FcToolClasses.MATERIAL.get(fcItem) : null;
                level = material instanceof btw.modern.EnumToolMaterial m ? m.getHarvestLevel() : -1;
            } catch (Throwable t) {
                logFcFailureOnce("toolMaterial", legacyId, t);
                level = -1;
            }
            cachedFcHarvestLevel = level;
        }
        return level;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (fcToolKind().digActions().contains(toolAction)) return true;
        return super.canPerformAction(stack, toolAction);
    }

    private volatile Multimap<Attribute, AttributeModifier> mainHandModifiers;

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        Multimap<Attribute, AttributeModifier> modifiers = mainHandModifiers;
        if (modifiers == null) {
            btw.modern.Item fcItem = fc();
            if (fcItem == null) return super.getDefaultAttributeModifiers(slot);
            int damage = fcItem.getDamageVsEntity(null);
            modifiers = damage > 1
                    ? ImmutableMultimap.of(
                            Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                                    damage - 1, AttributeModifier.Operation.ADDITION),
                            Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                                    0, AttributeModifier.Operation.ADDITION))
                    : super.getDefaultAttributeModifiers(slot);
            mainHandModifiers = modifiers;
        }
        return modifiers;
    }

    /**
     * FC's efficiency on 1.20.1-only blocks. Blocks with an FC identity dig through FC's own formula
     * (FCMiningSpeed), so only unmapped states are decided here, through FC's own flags on the stand-in FC block of
     * each mineable tag: a tag counts when the tool's family digs it, or FC's IsToolTypeEfficientVsBlockType
     * (Modern-Common Item.java:667) is true for the stand-in. #mineable/axe ~ FC log 17 (axes and chisels, Client
     * FCBlockLog.java:31-32; FCItemAxe also takes material GetAxesEfficientOn, Common FCItemAxe.java:157-160),
     * #mineable/pickaxe ~ FC stone 1 (picks and chisels, Client FCBlockStone.java:34-35), #mineable/shovel ~ FC
     * dirt 3 (shovels and hoes, Common FCBlockDirt.java:30-31). #mineable/hoe has no FC stand-in (FC hoes only
     * convert; FCItemHoe.canHarvestBlock is false), so the family alone decides it. FCItemTool has no 2-argument
     * getStrVsBlock override (javap), so the speed is the FC field efficiencyOnProperMaterial, never below the bare
     * hand's: FC's low-tier values (wood chisel 0.25, stone chisel 0.5, stone shovel 0.34) are balanced against FC's
     * divisor (an effective tool /30, a hand /200), while 1.20.1's divides a bare hand on a block that needs no tool by
     * 30 as well (BlockBehaviour.getDestroyProgress :369), so the raw value would dig slower than a hand.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_tool_actions: FC tools dig post-1.5.2 blocks at FC's efficiency instead of a bare hand's
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        btw.modern.Item fcItem = fc();
        if (fcItem != null && ProxyRegistry.getFcBlock(state.getBlock()) == null) {
            FcToolKind kind = FC_TOOL_KIND.get(fcItem.getClass());
            if (kind.tool() && fcToolEfficientOn(fcItem, kind, state)) {
                return Math.max(fcEfficiency(fcItem), base);
            }
        }
        return base;
    }

    /** FC stand-in blocks of the mineable tags: 1.5.2 log 17, stone 1, dirt 3. */
    private static final int FC_LOG_ID = 17, FC_STONE_ID = 1, FC_DIRT_ID = 3;

    private static boolean fcToolEfficientOn(btw.modern.Item fcItem, FcToolKind kind, BlockState state) {
        Set<ToolAction> dig = kind.digActions();
        if (state.is(BlockTags.MINEABLE_WITH_AXE)
                && (dig.contains(ToolActions.AXE_DIG) || fcToolTypeEfficientVs(fcItem, FC_LOG_ID))) {
            return true;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && (dig.contains(ToolActions.PICKAXE_DIG) || fcToolTypeEfficientVs(fcItem, FC_STONE_ID))) {
            return true;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                && (dig.contains(ToolActions.SHOVEL_DIG) || fcToolTypeEfficientVs(fcItem, FC_DIRT_ID))) {
            return true;
        }
        return state.is(BlockTags.MINEABLE_WITH_HOE)
                && dig.contains(ToolActions.HOE_DIG);
    }

    /** FC's own tool-type flag for the FC stand-in block {@code legacyBlockId}; false without that block. */
    private static boolean fcToolTypeEfficientVs(btw.modern.Item fcItem, int legacyBlockId) {
        return isFcBlockId(legacyBlockId)
                && fcItem.IsToolTypeEfficientVsBlockType(btw.modern.Block.blocksList[legacyBlockId]);
    }

    /**
     * Drops of 1.20.1-only blocks that need a tool tier (deepslate and its ores, copper, tuff, calcite, amethyst):
     * an FC tool qualifies when its family digs one of the state's mineable tags and FC's harvest level reaches the
     * block's tier, as DiggerItem.isCorrectToolForDrops (DiggerItem.java:80-82). Without it destroyBlock skipped
     * playerDestroy (ServerPlayerGameMode.java:249) and the steel pickaxe got nothing from deepslate diamond ore.
     * Blocks with an FC identity are FC's harvest (ServerPlayerGameModeMixin), so they are never decided here.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_tool_actions: post-1.5.2 blocks gate their drops on Forge's tool query, which FC tools never answered
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        btw.modern.Item fcItem = fc();
        if (fcItem != null && ProxyRegistry.getFcBlock(state.getBlock()) == null) {
            FcToolKind kind = FC_TOOL_KIND.get(fcItem.getClass());
            if (kind.tool() && fcFamilyDigs(kind, state)) {
                Tier tier = tierFor(fcHarvestLevel(fcItem));
                if (tier != null && TierSortingRegistry.isCorrectTierForDrops(tier, state)) {
                    return true;
                }
            }
        }
        return super.isCorrectToolForDrops(stack, state);
    }

    private static boolean fcFamilyDigs(FcToolKind kind, BlockState state) {
        Set<ToolAction> dig = kind.digActions();
        return (dig.contains(ToolActions.AXE_DIG)
                        && state.is(BlockTags.MINEABLE_WITH_AXE))
                || (dig.contains(ToolActions.PICKAXE_DIG)
                        && state.is(BlockTags.MINEABLE_WITH_PICKAXE))
                || (dig.contains(ToolActions.SHOVEL_DIG)
                        && state.is(BlockTags.MINEABLE_WITH_SHOVEL))
                || (dig.contains(ToolActions.HOE_DIG)
                        && state.is(BlockTags.MINEABLE_WITH_HOE));
    }

    /**
     * The vanilla tier of an FC harvest level (EnumToolMaterial: wood and gold 0, stone 1, iron 2, diamond 3,
     * soulforged steel 4); null when unreadable.
     */
    private static Tier tierFor(int harvestLevel) {
        if (harvestLevel < 0) return null;
        return switch (harvestLevel) {
            case 0 -> Tiers.WOOD;
            case 1 -> Tiers.STONE;
            case 2 -> Tiers.IRON;
            case 3 -> Tiers.DIAMOND;
            default -> Tiers.NETHERITE;
        };
    }

    // ================================================================
    // Stack-sensitive food query → FC food values (INTEGRATIONS #024)
    // ================================================================

    /**
     * FC's food values for FC-only foods (the donut, FC meats, ...), for mods that ask Forge's stack-sensitive food
     * query (AppleSkin #029, whose isFood is {@code getFoodProperties(stack, player) != null}; LSO #031).
     * Item.Properties keeps no food, so {@link #isEdible()} stays false and every vanilla eat path stays closed:
     * Item.use :141-142, getUseDuration :263-264, LivingEntity.eat :3274 and addEatEffect :3289-3290, FoodData.eat
     * :33-34, Wolf.isFood :407 (which also gates mobInteract :327), Cat :367-368. FC's onEaten (finishUsingItem)
     * stays the only eater.
     * One vanilla read has no isEdible guard, LivingEntity.shouldTriggerItemUseEffects :2911, which only looks at
     * isFastFood: FcFoodValues.toFoodProperties never sets fast() (FC has no fast foods), so the use particles and
     * sounds are unchanged.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_food_values: FC foods are not 1.20.1 foods, so Forge's food query answered null and food mods could not see them
    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        var food = btw.forge.food.FcFoodValues.of(stack, entity);
        FoodProperties props = food != null ? btw.forge.food.FcFoodValues.toFoodProperties(food) : null;
        return props != null ? props : super.getFoodProperties(stack, entity);
    }

    // ================================================================
    // Interact with entity → FC itemInteractionForEntity / useItemOnEntity
    // ================================================================

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                   LivingEntity target, InteractionHand hand) {
        if (fc() == null) return super.interactLivingEntity(stack, player, target, hand);
        // Server-authoritative, like ProxyAnimal.mobInteract: FC's client-side branch would act
        // on client puppets (FCItemBreedingHarness setDead()s the cow it replaces). The server's
        // SUCCESS swings the arm (ServerGamePacketListenerImpl.handleInteract).
        if (!player.level().isClientSide() && interactWithEntityFc(stack, target)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
