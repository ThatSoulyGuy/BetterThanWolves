package btw.forge.mixin;

import btw.forge.BoneMealPolicy;
import btw.forge.ContainerBridge;
import btw.forge.InventoryBridge;
import btw.forge.ItemStackHelper;
import btw.forge.LegacyProxyItem;
import btw.forge.PlayerBridge;
import btw.forge.ProxyBlock;
import btw.forge.ProxyBlockEntity;
import btw.forge.ProxyItem;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_harvest_events (INTEGRATIONS #022, FC actions visible to other mods)
 *
 * <p><b>Feature:</b> 1.20.1 / Forge's player block-break contract, {@code ServerPlayerGameMode.destroyBlock}
 * (ServerPlayerGameMode.java:229-262): it posts {@code BlockEvent.BreakEvent} through
 * {@code ForgeHooks.onBlockBreakEvent} (ForgeHooks.java:513-565) before anything is removed, which claim and
 * protection mods cancel and LSO thirst / Cold Sweat listen to; that call also pre-cancels for spectators, for
 * adventure players without a CanDestroy tag and for a held item whose {@code canAttackBlock} is false (a sword in
 * creative). It then honours {@code IForgeItem.onBlockStartBreak} for the held item. The same class carries two
 * more hook groups: right-click on a block ({@code useItemOn}: the click frame at HEAD, FC {@code onItemUse} at
 * 1.20.1's item step, FC's own placement finished at RETURN; seams fc_item_use_order and placement_validity, below)
 * and right-click in air ({@code useItem}, FC {@code onItemRightClick}).</p>
 *
 * <p><b>Conflict with FC:</b> FC's harvest pipeline ({@code ItemInWorldManager.tryHarvestBlock}: FC
 * {@code canHarvestBlock}, then {@code CanConvertBlock}/{@code ConvertBlock} (log to chewed log, bark), then
 * {@code harvestBlock} or {@code OnBlockDestroyedWithImproperTool}) reads the block from the world, so it must run
 * BEFORE vanilla removes it: this mixin cancels {@code destroyBlock} at HEAD for every block with an FC identity.
 * That cancel also skipped the BreakEvent, so claim, protection and thirst handlers never saw an FC harvest, and
 * direct {@code destroyBlock} callers (machines, fake players, vein-miners; {@code handleBlockBreakAction} checks
 * {@code blockActionRestricted} itself at START, ServerPlayerGameMode.java:149) bypassed the spectator and
 * adventure rules. The FC pipeline had no creative branch either, so creative breaks chewed logs, dropped items and
 * wore tools (1.5.2 just removes the block, vanilla/server ItemInWorldManager.java:421-444).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): the FC harvest keeps its HEAD cancel and runs inside Forge's
 * contract, posting the BreakEvent and honouring {@code onBlockStartBreak} in vanilla's order, then hands FC the
 * player's game mode so FC's own 1.5.2 creative branch decides creative breaks. Exception (vanilla_pair_drops,
 * maintainer ruling 2026-10-20): a bed or door, the blocks of {@link btw.forge.VanillaPairDrops#isRuled}, keeps
 * 1.20.1's {@code destroyBlock} (its BreakEvent, {@code playerWillDestroy}, loot and partner pop); only FC's animal
 * panic runs inside it ({@code btw$afterPairRemoved}).</p>
 *
 * <p><b>Rebalance:</b> none; 4.B parity plus 1.20.1's break rules. A cancelled BreakEvent (or an
 * {@code onBlockStartBreak} that claims the break) leaves the FC block in place and returns false, so
 * {@code destroyAndAck} resends it. Another mod's BreakEvent listener or {@code onBlockStartBreak} that throws
 * refuses the break the same way (logged once per block id, the block resent), which is vanilla's effective outcome
 * without the per-hit FATAL task log or a LinkageError crash. Creative with a sword can no longer break FC blocks
 * (the 1.20.1 rule for every block). Adventure keeps 1.20.1's rule (CanDestroy), not 1.5.2's {@code canCurrentToolHarvestBlock} clause. The
 * event fires once per FC destroy call, including FC conversion stages: each log-chewing hit (Client
 * FCBlockLog.ConvertBlock) is one BreakEvent, so a thirst mod charges per hit. The event's XP is ignored: FC drops
 * its own XP in {@code harvestBlock}; the BreakEvent constructor's {@code getExpDrop} draw uses
 * {@code level.random}, and FC draws from WorldBridge's own {@code java.util.Random} (WorldBridge.java:108), so FC's
 * random stream is unchanged.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at HEAD of {@code destroyBlock(BlockPos)}, cancellable, as before.
 * For any other block with an FC identity it posts the BreakEvent and checks {@code onBlockStartBreak} first, then
 * runs FC's {@code tryHarvestBlock}. {@code GameMasterBlock} is unreachable (legacy 137, the command block, has no
 * FC block) and {@code blockActionRestricted} (Player.java:186-197) is the condition the BreakEvent pre-cancel
 * already applies, so neither is repeated. Blocks without an FC identity (post-1.5.2 blocks, twin plants, the bark
 * and stripped log forms, Dynamic Trees blocks) and the ruled beds and doors (vanilla_pair_drops) keep vanilla
 * {@code destroyBlock}, which posts the event itself (:229), so the event never fires twice. For those beds and
 * doors {@code btw$afterPairRemoved}, a RETURN inject into Forge's private {@code removeBlock(BlockPos, boolean)}
 * (ServerPlayerGameMode.java:268-274; not obfuscated, hence {@code remap = false}), reads the removed state from
 * its local and runs FC's animal panic after the removal and before the drops, as FC's own harvest does. It keeps
 * no field, so a {@code destroyBlock} nested in a BreakEvent handler (vein miners) cannot clear another call's
 * record. The {@code useItemOn} and {@code useItem} injects are separate seams (fc_item_use_order and the
 * right-click in air, below) that this one does not touch.</p>
 *
 * <p><b>Alternative considered and rejected:</b> running the FC harvest after Forge's event, at a later point of
 * vanilla's {@code destroyBlock}: by then vanilla has removed the block and spawned its drops, and FC's
 * {@code tryHarvestBlock} reads the block from the world, which is the reason for the HEAD cancel. Posting the event
 * from a BreakEvent listener of BTW's own: it would not see direct callers' game modes or onBlockStartBreak, and
 * would order differently from every other block.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_harvest_events: an FC harvest cancels vanilla destroyBlock, so it must post Forge's BreakEvent and honour onBlockStartBreak itself
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow protected ServerLevel level;
    @Shadow protected ServerPlayer player;

    // FCMOD-INTEGRATION(1.20.1): vanilla_pair_drops: FC's animals hear a player's removal of a bed or door that 1.20.1 breaks, as in FC's own harvest (Modern-Common ItemInWorldManager.java:142-146): after the removal, before the drops
    // Forge's private removeBlock (ServerPlayerGameMode.java:268-274) has one return; state and removed are its locals
    // there, read per call (no field: a destroyBlock nested in a BreakEvent handler cannot clear this call's record).
    // Not obfuscated, so remap = false. Only ruled states reach it with an FC identity (btw$destroyBlock cancels the rest).
    @Inject(method = "removeBlock", at = @At("RETURN"), remap = false,
            locals = org.spongepowered.asm.mixin.injection.callback.LocalCapture.CAPTURE_FAILHARD)
    private void btw$afterPairRemoved(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir,
                                      BlockState removedState, boolean removed) {
        if (removed && btw.forge.VanillaPairDrops.isRuled(removedState)) {
            btw.forge.VanillaPairDrops.afterPlayerBreak(player, level, pos, removedState);
        }
    }

    @Unique private BlockPos btw$digPos;
    @Unique private int btw$digFace = 1;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void btw$recordDigFace(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                                   int maxBuildHeight, int sequence, CallbackInfo ci) {
        btw$digPos = pos.immutable();
        btw$digFace = face.get3DDataValue();
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void btw$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        btw.modern.Block fcBlock = ProxyRegistry.getFcBlock(state.getBlock());
        if (fcBlock == null) return;
        if (btw.forge.VanillaPairDrops.isRuled(state) || WorldBridge.isCollapsedFlower(state)) return;

        ServerPlayerGameMode self = (ServerPlayerGameMode) (Object) this;
        GameType gameMode = self.getGameModeForPlayer();
        boolean refused;
        try {
            refused = ForgeHooks.onBlockBreakEvent(level, gameMode, player, pos) == -1
                    || player.getMainHandItem().onBlockStartBreak(pos, player);
        } catch (RuntimeException | LinkageError e) {
            ProxyItem.logFcFailureOnce("BreakEvent/onBlockStartBreak of another mod (legacy block id)",
                    fcBlock.blockID, e);
            if (player.connection != null) {
                player.connection.send(new ClientboundBlockUpdatePacket(level, pos));
            }
            refused = true;
        }
        if (refused) {
            cir.setReturnValue(false);
            return;
        }

        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(player);
        fcPlayer.syncFromReal();

        btw.modern.ItemInWorldManager mgr = new btw.modern.ItemInWorldManager(
                WorldBridge.getOrCreate(level));
        mgr.thisPlayerMP = fcPlayer;
        mgr.setGameType(btw.modern.EnumGameType.getByID(gameMode.getId()));

        boolean handled = true;
        try {
            mgr.tryHarvestBlock(pos.getX(), pos.getY(), pos.getZ(), pos.equals(btw$digPos) ? btw$digFace : 1);
        } catch (Throwable t) {
            ProxyItem.logFcFailureOnce("tryHarvestBlock (legacy block id)", fcBlock.blockID, t);
            handled = level.getBlockState(pos) != state;
        }

        if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
            invBridge.writeBackCurrentItem(mgr.lastHeldStack);
        }

        cir.setReturnValue(handled);
    }

    // ================================================================
    // useItemOn: one right-click on a block (fc_item_use_order)
    // ================================================================

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_item_use_order
     *
     * <p><b>Feature:</b> 1.20.1 / Forge's order and gates for a right-click on a block,
     * {@code ServerPlayerGameMode.useItemOn} (ServerPlayerGameMode.java:317-370): the RightClickBlock event (:323; a
     * cancel ends the click, :324), the spectator branch (:325-332), {@code onItemUseFirst} (:335-338), the sneak rule
     * (:339-340), the clicked block's {@code use} (:343, which ends the click when it consumes, :344-347), then, unless
     * the event denied the item (:351) or it is on cooldown (:350), the item step {@code ItemStack.useOn} (creative
     * :355, survival :358). Apart from the off hand and the Forge gates this is 4.B's order:
     * ItemInWorldManager.activateBlockOrUseItem activates the block unless the player sneaks with an item, and uses the
     * held item only when the activation declined (vanilla/server ItemInWorldManager.java:376-407).</p>
     *
     * <p><b>Conflict with FC:</b> FC's {@code onItemUse} ran at HEAD of useItemOn, ahead of all of it, so that vanilla
     * items with FC replacements would also reach blocks without Stage 2. Whenever the FC item acted it consumed the
     * click before any activation, FC or 1.20.1: flint and steel on a lever or on weedy FC crops, an axe on a note
     * block, any item on a millstone. It also ran before the event (claim mods' cancel and DENY did not stop it) and the
     * spectator check, it ran a second time after Stage 2 declined on the same block, it wrote back only the selected
     * slot, and neither dispatch point awarded 4.B's item-used stat (vanilla/server ItemStack.java:127-130).</p>
     *
     * <p><b>Integration pattern:</b> D (FC-extend): FC's item use runs where 1.20.1 calls the held item, and nowhere
     * else. One click runs at most one FC block activation and at most one FC item use, at whichever comes first of
     * Stage 2 (ProxyBlock.use / BlockBehaviorMixin.btw$use, inside block use) and the item step (here), main hand only,
     * through the single FC {@code onItemUse} point {@code ProxyBlock.tryPlaceHeldItem} (the DENY check, the per-click
     * mark, the 1.5.2 creative restore, the stat) and one write-back routine. A click frame on the {@code PlayerBridge}
     * carries the DENY verdict (FcItemUseOrder) and the mark between them.</p>
     *
     * <p><b>Rebalance:</b> none; 4.B's order. Holding a tool, flint and steel or a stick while right-clicking a lever,
     * a door, a chest, a crop or an FC machine activates it; placing a tool into such a block takes a sneak-click, as
     * in 4.B. Under a {@code useItem} DENY, Stage 2 no longer places FC blocks or runs FCItemDye. Residuals where
     * 1.20.1's {@code use} of a vanilla class with an FC identity now comes before the FC item (redstone ore, an empty
     * flower pot, signs, a tuning fork on a note block; TNT primed by flint and steel costs 1 durability, a fire charge
     * primes it at once) are accepted pending the activation audit (INTEGRATIONS #033).</p>
     *
     * <p><b>Injection strategy:</b> {@link Inject} at HEAD opens the click frame and never cancels
     * ({@code btw$beginClick}); a cancellable {@link Inject} at INVOKE {@code ItemStack.useOn} with {@code require = 2}
     * runs FC's item use for the blocks Stage 2 did not serve, and refuses 1.20.1's BlockItem fallback where Stage 2's
     * FC placement refused (placement_validity) ({@code btw$fcItemStep}); the RETURN inject finishes a block FC's Stage 2
     * placed and closes the frame ({@code btw$afterUseItemOn}). The item step comes after the event,
     * the spectator check, the sneak rule, block use, the DENY return and the cooldown, and before Forge's adventure
     * gate and snapshot capture (ForgeHooks.onPlaceItemIntoWorld, ForgeHooks.java:573, :584), whose adventure rule FC's
     * own {@code canPlayerEdit} already applies (Modern-Common EntityPlayer.java:292-299). Callback injectors are
     * applied before {@code @Redirect}s, so another mod's redirect of the same call stays compatible.</p>
     *
     * <p><b>Alternative considered and rejected:</b> FC inside {@code ProxyItem.useOn} plus an {@code Item.useOn}
     * mixin, which would run FC under the snapshot capture that defers {@code onPlace} (ForgeHooks.java:584, :644);
     * keeping HEAD and re-implementing the event, spectator, sneak and block-use steps there; HEAD of
     * {@code ItemStack.useOn}, which has no click boundary for the per-click state (deployers never call useItemOn) and
     * changes every third-party caller; a {@code @Redirect} of {@code useOn}, which is exclusive and fails against
     * another mod's redirect.</p>
     */
    // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: the click frame opens first, before any early return of useItemOn
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void btw$beginClick(ServerPlayer serverPlayer, Level world, ItemStack mcStack, InteractionHand hand,
                                BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerBridge.getOrCreate(serverPlayer).beginUseClick(serverPlayer.server.getTickCount());
    }

    /**
     * 1.20.1's item step (ServerPlayerGameMode.java:350-358) for an FC item: FC {@code onItemUse} for a non-block item
     * with an FC identity (FC items, and vanilla items FC replaces: hoes, shears, flint and steel, tools), used in the
     * main hand on a block whose activation did not consume the click. That covers every block without Stage 2 (a
     * 1.20.1 class that overrides {@code use}, or a block with no FC identity), every block when the player sneaks with
     * an item (4.B: vanilla/server ItemInWorldManager.java:380), and a block whose {@code use} declined without running
     * Stage 2 (Dynamic Trees' branches return a non-consuming FAIL, DT BranchBlock.java:196-203). FC true ends the
     * click (CONSUME); FC false lets 1.20.1's {@code useOn} run, as before (an axe strip or a hoe till where FC
     * declined).
     */
    // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: FC onItemUse runs at 1.20.1's item step, after the event, the spectator check, the sneak rule and block use (4.B's order)
    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"),
            cancellable = true, require = 2)
    private void btw$fcItemStep(ServerPlayer serverPlayer, Level world, ItemStack mcStack, InteractionHand hand,
                                BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (mcStack.isEmpty() || !(world instanceof ServerLevel sl)) return;
        // The FC player exposes only the main hand (InventoryBridge.getCurrentItem; 1.5.2 has no off hand). The
        // off-hand item's FC code would act on, and write back, the main-hand snapshot (an off-hand FC seed planted by
        // decrementing the main-hand tool's stack). The off hand stays vanilla.
        if (hand != InteractionHand.MAIN_HAND) return;
        // BlockItems place through 1.20.1 here (their FC route is Stage 2), unless Stage 2 refused them.
        if (mcStack.getItem() instanceof BlockItem) {
            // FCMOD-INTEGRATION(1.20.1): placement_validity: FC's Stage 2 refusal of a 4.B ItemBlock-family placement is final; 1.20.1's BlockItem does not place it instead
            // 4.B's client never sent a placement its own check refused (vanilla/client PlayerControllerMP.java:425-433,
            // FCItemPlacesAsBlock.CanItemBeUsedByPlayer -> canPlaceItemBlockOnSide, Common :132-135, :180-203), so the
            // server's refusal was the end. Here the 1.20.1 client predicted the placement and shrank its stack: the held
            // slot is resent (INV-RESYNC's resendHeldSlot; the ghost block is cleared by the two block updates
            // ServerGamePacketListenerImpl sends after every useItemOn). Not keyed on every FC item: a door or sign
            // "false" is a port gap (ProxyBlock.isItemBlockFamily), and deployers that call useOn directly are untouched.
            PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
            // Tied to the clicked block: a foreign blockstate.use nested in this click that runs Stage 2 elsewhere records
            // a verdict that is not this click's
            if (fcPlayer.stage2() == PlayerBridge.Stage2.REFUSED && fcPlayer.fcItemUseRanAt(hitResult.getBlockPos())) {
                if (fcPlayer.inventory instanceof InventoryBridge invBridge) invBridge.resendHeldSlot();
                // A RETURN inject does not run after an injector cancels: close this click's frame before the cancel
                fcPlayer.endUseClick();
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        // ProxyItems (FC non-block items: tools, seeds, ...) and vanilla items with FC replacements (hoe, shears, ...)
        int legacyId = ProxyRegistry.getItemId(mcStack.getItem());
        if (legacyId <= 0 || legacyId >= btw.modern.Item.itemsList.length) return;
        btw.modern.Item fcItem = btw.modern.Item.itemsList[legacyId];
        if (fcItem == null) return;

        BlockPos pos = hitResult.getBlockPos();
        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);   // synced from the real player
        // Stage 2 already ran FC onItemUse at this block in this click and FC declined: 1.20.1's useOn runs as the
        // fallback, as before, and FC never runs twice in one click.
        if (fcPlayer.fcItemUseRanAt(pos)) return;
        btw.modern.ItemStack fcStack = fcPlayer.getCurrentEquippedItem();
        if (fcStack == null) return;

        // FCMOD-INTEGRATION(1.20.1): bone_meal: bone meal keeps BoneMealItem.useOn at the item step, its one route for sneaking, the off hand and targets without Stage 2
        // FC dye 15 is bone meal (FCItemDye.onItemUse acts on nothing else, Client FCItemDye.java:34-47). Running it here
        // would put FCItemDye ahead of BoneMealItemMixin (BoneMealPolicy.onBoneMealUseOn: the soil relay for 1.20.1
        // plants and the client prediction). Activation still comes first, 4.B's order (vanilla/server
        // ItemInWorldManager.java:376-407): Stage 1, then Stage 2, which runs FCItemDye inside block use. 1.20.1 white
        // dye reads as FC (351, 15|0x100) = 271 through LegacyVariants.MODERN_SPELLING, never FC dye 15; isFcBoneMeal
        // stays for bone meal's 4.B activation order. White dye takes this step like any other dye, and FCItemDye
        // declines it (Client FCItemDye.java:36).
        if (BoneMealPolicy.isFcBoneMeal(fcItem, fcStack)) return;

        int side = hitResult.getDirection().get3DDataValue();
        Vec3 hitLoc = hitResult.getLocation();
        float hitX = (float) (hitLoc.x - pos.getX());
        float hitY = (float) (hitLoc.y - pos.getY());
        float hitZ = (float) (hitLoc.z - pos.getZ());

        // Snapshot openContainer before the FC call to detect GUI opens; the criterion gets the stack as clicked
        btw.modern.Container prevContainer = fcPlayer.openContainer;
        ItemStack used = mcStack.copy();
        // The single FC onItemUse point: the DENY check, the per-click mark, the 1.5.2 creative restore (it replaces the
        // count restore at :355-356 that the cancel below skips) and 4.B's ITEM_USED stat
        boolean result = ProxyBlock.tryPlaceHeldItem(fcStack, serverPlayer, fcPlayer, WorldBridge.getOrCreate(sl),
                pos, side, hitX, hitY, hitZ);
        ContainerBridge.checkAndOpenContainer(fcPlayer, prevContainer);
        // Stage 2's write-back: every slot FC changed (InventoryBridge.writeBackAll), not only the selected one
        fcPlayer.syncInventoryToReal();
        if (!result) return;
        // INV-RESYNC: as Stage 2 (ProxyBlock.use), the client sees the held stack FC left once it is written back, as
        // after every 1.5.2 place packet (NetServerHandler.java:551-569)
        if (fcPlayer.inventory instanceof InventoryBridge invBridge) invBridge.resendHeldSlot();

        // As Stage 2 (ProxyBlock.use): send the FC block entity's change (cook stack, spit, metadata) at once
        if (sl.getBlockEntity(pos) instanceof ProxyBlockEntity pbe) {
            pbe.syncToClients();
        }
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, used);
        // A RETURN inject does not run after an injector cancels: close this click's frame before the cancel
        fcPlayer.endUseClick();
        cir.setReturnValue(InteractionResult.CONSUME);
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void btw$afterUseItemOn(ServerPlayer serverPlayer, Level world,
                                    ItemStack mcStack, InteractionHand hand,
                                    BlockHitResult hitResult,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
        try {
            if (!(world instanceof ServerLevel)) return;
            if (fcPlayer.stage2() != PlayerBridge.Stage2.PLACED) return;
            net.minecraft.core.BlockPos placePos = fcPlayer.stage2At();
            if (placePos == null) return;
            net.minecraft.world.level.block.state.BlockState placed = world.getBlockState(placePos);

            if (!(placed.getBlock() instanceof net.minecraft.world.level.block.TripWireHookBlock)
                    && !(placed.getBlock() instanceof net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock)) {
                net.minecraft.world.level.block.state.BlockState fixed = placed;
                if (placed.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
                    net.minecraft.core.Direction correct = net.minecraft.core.Direction.orderedByNearest(serverPlayer)[0].getOpposite();
                    fixed = placed.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, correct);
                } else if (placed.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                    net.minecraft.core.Direction correct = serverPlayer.getDirection().getOpposite();
                    fixed = placed.setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, correct);
                }
                if (!fixed.equals(placed) && fixed.canSurvive(world, placePos)) {
                    world.setBlock(placePos, fixed, 2);
                }
            }

            net.minecraft.world.level.block.state.BlockState now = world.getBlockState(placePos);
            if ((now.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
                    || now.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock)
                    && now.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    && !now.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)
                    && world.hasNeighborSignal(placePos)) {
                world.setBlock(placePos, now
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true)
                        .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, true), 2);
            }
        } finally {
            fcPlayer.endUseClick();
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

        // ProxyItems dispatch FC onItemRightClick themselves: useItem goes on to ItemStack.use ->
        // ProxyItem.use (ServerPlayerGameMode.java:286), which runs it main-hand only, applies FC's
        // returned stack (1.5.2 ItemInWorldManager.tryUseItem) and writes the inventory back. Running
        // it here as well ran FC twice per click: soul urns, dynamite and soul-sand piles threw two
        // entities for one consumed item, FC buckets and the baited fishing rod acted twice.
        // ProxyArmorItems lose nothing: Modern-Common ItemArmor has no onItemRightClick (MC
        // ArmorItem.use equips).
        if (mcStack.getItem() instanceof LegacyProxyItem) return;
        // The FC player exposes only the main hand (1.5.2 has no off hand): an off-hand use ran the
        // off-hand item's FC code on the main-hand snapshot (getCurrentEquippedItem below).
        if (hand != InteractionHand.MAIN_HAND) return;

        // Look up the FC item for whatever the player is holding (the 1.5.2 item id first: carrots,
        // seeds, string, ... are BlockItems in 1.20.1, see ProxyItem.fcItemFor)
        btw.modern.Item fcItem = ProxyItem.fcItemFor(mcStack.getItem());
        if (fcItem == null) return; // No FC item — let vanilla handle it
        // Creative: FC's ItemFood.onItemRightClick refuses (EntityPlayer.canEat is false while
        // disableDamage) and plays OnCantConsume, but 1.20.1 Player.canEat lets an invulnerable
        // player eat and vanilla Item.use starts it anyway. Leave creative eating to vanilla, silently.
        if (fcItem instanceof btw.modern.ItemFood && serverPlayer.getAbilities().invulnerable) return;

        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
        fcPlayer.syncFromReal();
        btw.modern.World fcWorld = WorldBridge.getOrCreate(level);

        btw.modern.ItemStack fcStack = fcPlayer.getCurrentEquippedItem();
        if (fcStack == null) return;

        // Call FC's onItemRightClick — FC decides what happens
        btw.modern.ItemStack result;
        // INV-RESYNC: vanilla's use runs next and owns this stack (the items-wave caveat: FC's returned stack is not
        // applied). A sync nested in FC's call keeps FC's objects but writes none of its edits (shadow scope), and they
        // are dropped when FC returns, as every sync dropped them before, never written on top of vanilla's.
        InventoryBridge shadow = fcPlayer.inventory instanceof InventoryBridge ib ? ib : null;
        if (shadow != null) shadow.beginShadow();
        try {
            result = fcItem.onItemRightClick(fcStack, fcWorld, fcPlayer);
        } catch (Throwable t) {
            // Packet handler: PacketUtils rethrows, which crashes the integrated server.
            ProxyItem.logFcFailureOnce("onItemRightClick", fcItem.itemID, t);
            return;
        } finally {
            if (shadow != null) {
                shadow.discardPendingEdits();
                shadow.endShadow();
            }
        }

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
