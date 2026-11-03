package btw.forge.mixin;

import btw.forge.BoneMealPolicy;
import btw.forge.ContainerBridge;
import btw.forge.DigFace;
import btw.forge.DigFaceAccess;
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
public abstract class ServerPlayerGameModeMixin implements DigFaceAccess {

    @Shadow protected ServerLevel level;
    @Shadow protected ServerPlayer player;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos delayedDestroyPos;

    // ================================================================
    // The dig face (fc_dig_face): the face FC's harvest converts toward
    // ================================================================

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_dig_face
     *
     * <p><b>Feature:</b> 1.20.1's dig protocol, {@code handleBlockBreakAction} (ServerPlayerGameMode.java:126-215): every
     * START_DESTROY_BLOCK and STOP_DESTROY_BLOCK packet carries the face the player is digging, and the block breaks
     * inside that call ({@code destroyAndAck}: a creative or insta-mine START, :145, :165; a STOP with 0.7 of the
     * progress, :188) or later, in {@code tick}'s delayed destroy (:87-97) that a STOP short of 0.7 schedules
     * (:192-197).</p>
     *
     * <p><b>Conflict with FC:</b> FC's harvest takes that face ({@code tryHarvestBlock(i, j, k, iFromSide)}, Modern-Common
     * ItemInWorldManager.java:35, :88) and every FC conversion ejects its items out of it
     * ({@code FCUtilsItem.EjectStackFromBlockTowardsFacing}, Common FCUtilsItem.java:118-158: stone and rough stone
     * under a pickaxe that cannot harvest them, the staged ores, a hoe on grass, chewed logs and their bark, the work
     * stump). {@code destroyBlock(BlockPos)} carries no face, so the port passed 1 (UP) for every break: the items
     * spawned inside the block above, and 1.20.1's push (ItemEntity.java:131-133, Entity.moveTowardsClosestSpace
     * :2343) carried them through rock to a random open side, or up through the ceiling (triage CF2-DR2).</p>
     *
     * <p><b>Integration pattern:</b> D (FC-extend), 4.B parity: the face of the dig packet that breaks the block,
     * recorded per position for the duration of that {@code handleBlockBreakAction} call, and the STOP packet's face
     * kept for the delayed destroy, as 1.5.2's ItemInWorldManager kept {@code m_iHarvestingFromFacing} (vanilla/server
     * ItemInWorldManager.java:44, :120, :155-188, :208-241). {@link DigFace#of} reads it; a break without a dig packet
     * takes the look ray's face, then UP.</p>
     *
     * <p><b>Rebalance:</b> none.</p>
     *
     * <p><b>Injection strategy:</b> {@link Inject} at HEAD and RETURN of {@code handleBlockBreakAction} (record, then
     * clear; a STOP that set {@code hasDelayedDestroy} in this call keeps its face for the delayed destroy) and around
     * {@code tick}'s single {@code destroyBlock} call (INVOKE, and INVOKE shifted AFTER). None cancels, so Forge's
     * LeftClickBlock event, vanilla's reach, height and spawn-protection checks and other mods' injects into these
     * methods are untouched. The record is read by position only, so a {@code destroyBlock} of another cell nested in a
     * BreakEvent handler (vein miners) never takes this dig's face. A LeftClickBlock or BreakEvent listener that calls
     * {@code handleBlockBreakAction} again on the same game mode (nested) replaces the record and clears it at its
     * RETURN, so the outer break takes the look ray's face: a safe fallback for a rare case. Per player, server thread
     * only, never saved.</p>
     *
     * <p><b>Alternative considered and rejected:</b> the look ray alone ({@link btw.forge.FCLogRules#hitFace}'s old
     * rule): the server's view of the player's rotation can lag the dig packet, and a delayed destroy finishes ticks
     * after the player looked away. A field read by every {@code destroyBlock}: a direct caller (a deployer, a vein
     * miner) would inherit a stale face from the player's last dig.</p>
     */
    // FCMOD-INTEGRATION(1.20.1): fc_dig_face: the dig packet's face, recorded for the cell it breaks while handleBlockBreakAction runs, and kept for tick()'s delayed destroy
    @Unique private BlockPos btw$digPos;
    @Unique private int btw$digFaceId = -1;
    @Unique private int btw$delayedFaceId = -1;
    @Unique private boolean btw$hadDelayedDestroy;

    // FCMOD-INTEGRATION(1.20.1): fc_dig_face: a START or STOP packet names the face of the cell it digs (ABORT names none)
    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void btw$digBegin(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                              int maxBuildHeight, int sequence, CallbackInfo ci) {
        boolean dig = face != null && (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
        this.btw$digPos = dig ? pos.immutable() : null;
        this.btw$digFaceId = dig ? face.get3DDataValue() : -1;
        this.btw$hadDelayedDestroy = this.hasDelayedDestroy;
        // tick() drops a delayed destroy whose cell turned to air without calling destroyBlock: no face survives it
        if (!this.hasDelayedDestroy) this.btw$delayedFaceId = -1;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_dig_face: a STOP short of 0.7 schedules the destroy for tick(); 1.5.2 kept that STOP's face for it (m_iHarvestingFromFacing, vanilla/server ItemInWorldManager.java:236-240)
    // Runs at every return: the LeftClickBlock cancel (:128-130), too far, too high, and the START and STOP branches.
    @Inject(method = "handleBlockBreakAction", at = @At("RETURN"))
    private void btw$digEnd(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face,
                            int maxBuildHeight, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK && !this.btw$hadDelayedDestroy
                && this.hasDelayedDestroy) {
            this.btw$delayedFaceId = this.btw$digFaceId;
        }
        this.btw$digPos = null;
        this.btw$digFaceId = -1;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_dig_face: tick()'s delayed destroy (ServerPlayerGameMode.java:95) breaks with the face its STOP packet carried
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private void btw$delayedDigBegin(CallbackInfo ci) {
        this.btw$digPos = this.delayedDestroyPos;
        this.btw$digFaceId = this.btw$delayedFaceId;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
            shift = At.Shift.AFTER))
    private void btw$delayedDigEnd(CallbackInfo ci) {
        this.btw$digPos = null;
        this.btw$digFaceId = -1;
        this.btw$delayedFaceId = -1;
    }

    @Override
    public int btw$digFace(BlockPos pos) {
        BlockPos at = this.btw$digPos;
        return at != null && at.equals(pos) ? this.btw$digFaceId : -1;
    }

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

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void btw$destroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = level.getBlockState(pos);
        btw.modern.Block fcBlock = ProxyRegistry.getFcBlock(state.getBlock());
        if (fcBlock == null) return; // Not an FC block — let vanilla handle it
        // FCMOD-INTEGRATION(1.20.1): vanilla_pair_drops: a bed or door (one item, two cells) keeps 1.20.1's destroyBlock: its BreakEvent, playerWillDestroy (creative partner removal), loot and partner pop (maintainer ruling 2026-10-20; triage CF2-DR1, CF2-DR6)
        // No cancel: vanilla posts the BreakEvent itself (:229); btw$afterPairRemoved runs FC's animal panic.
        if (btw.forge.VanillaPairDrops.isRuled(state)) return;

        // FCMOD-INTEGRATION(1.20.1): fc_dig_face: the face FC's conversions eject toward (DigFace: the dig packet's, else the look ray's, else up); read before the BreakEvent, whose handlers may break other cells
        int fromSide = DigFace.of(player, pos).get3DDataValue();

        // FCMOD-INTEGRATION(1.20.1): fc_harvest_events: Forge's break contract in vanilla's order (destroyBlock:229-242) before the FC pipeline, so claim, protection and thirst handlers see FC harvests
        // onBlockBreakEvent pre-cancels spectators, adventure without CanDestroy and canAttackBlock=false (a creative
        // sword), sends the client the air update first and resends the block (and its BE data) on a cancel. Its XP
        // is ignored: FC drops its own. Fires once per FC destroy call, conversion stages (log chewing) included.
        ServerPlayerGameMode self = (ServerPlayerGameMode) (Object) this;
        GameType gameMode = self.getGameModeForPlayer();
        boolean refused;
        try {
            refused = ForgeHooks.onBlockBreakEvent(level, gameMode, player, pos) == -1
                    || player.getMainHandItem().onBlockStartBreak(pos, player);
        } catch (RuntimeException | LinkageError e) {
            // FCMOD-INTEGRATION(1.20.1): fc_harvest_events: a third-party BreakEvent listener or onBlockStartBreak that throws refuses the break (fail closed) instead of escaping into the packet handler
            // Vanilla's effective outcome (the throw aborts destroyBlock, the block stays) without the FATAL task log
            // per hit or a LinkageError crash; a protection listener that failed is never bypassed. The air update
            // onBlockBreakEvent may already have sent is undone here, also for direct callers that get no resend.
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

        // FC's complete harvest pipeline — block is still in the world at this point
        btw.modern.ItemInWorldManager mgr = new btw.modern.ItemInWorldManager(
                WorldBridge.getOrCreate(level));
        mgr.thisPlayerMP = fcPlayer;
        // FCMOD-INTEGRATION(1.20.1): fc_harvest_events: FC's 1.5.2 creative branch keys on the manager's game type (vanilla/server ItemInWorldManager.java:421-444); 1.20.1 GameType ids 0-2 are 1.5.2's
        // Spectator (3) never gets here: the BreakEvent pre-cancel above refuses it. setGameType only stores it
        // (Modern-Common), the abilities stay 1.20.1's.
        mgr.setGameType(btw.modern.EnumGameType.getByID(gameMode.getId()));

        boolean handled = true;
        // FCMOD-INTEGRATION(1.20.1): collapsed_identity: FC's harvest removes the block before it drops it (Modern-Common ItemInWorldManager.tryHarvestBlock), so the self-drop reads the removed state from this per-thread record
        Object harvestRecord = btw.forge.CollapsedIdentity.beginHarvest(level, pos, level.getBlockState(pos));
        try {
            mgr.tryHarvestBlock(pos.getX(), pos.getY(), pos.getZ(), fromSide);
        } catch (Throwable t) {
            // Packet handler: PacketUtils rethrows, which crashes the integrated server. FC may already
            // have removed/converted the block, dropped items or worn the tool, so vanilla must not run
            // its own removal/drops on top: still write back and cancel. Report success only if the block
            // changed; false makes destroyAndAck resend the block, undoing the client's predicted break.
            ProxyItem.logFcFailureOnce("tryHarvestBlock (legacy block id)", fcBlock.blockID, t);
            handled = level.getBlockState(pos) != state;
        } finally {
            btw.forge.CollapsedIdentity.endHarvest(harvestRecord);
        }

        // Sync FC item state (damage, stackSize) back to MC inventory.
        // mgr.lastHeldStack is the FC ItemStack that was modified inside
        // tryHarvestBlock (damageItem, stackSize changes); null after a throw, which writes the
        // held slot's own FC state.
        if (fcPlayer.inventory instanceof InventoryBridge invBridge) {
            invBridge.writeBackCurrentItem(mgr.lastHeldStack);
        }

        // Tell vanilla we handled it — don't let vanilla do its own removal/drops
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

    /**
     * Runs after useItemOn completes, at each of its returns. When this click's FC Stage 2 placed the held block
     * (the click frame's PLACED record, ProxyBlock.tryPlaceHeldItem), it finishes that block, as 1.20.1's
     * getStateForPlacement would have: the look-based facing, for the Modern-Common bases that do not yet set one
     * (no onBlockPlacedBy on BlockPumpkin, no onBlockPlaced on BlockTrapDoor, BlockLever, BlockButton), and the redstone
     * state of a trapdoor or fence gate placed against a live signal. Last, it closes the click frame btw$beginClick
     * opened (fc_item_use_order).
     *
     * <p>FCMOD-INTEGRATION(1.20.1): placement_validity (decision 6(b)): the facing is written only for FC's own
     * placement and only to a facing the block survives in. It used to be written after EVERY BlockItem useItemOn:
     * 1.20.1's own placements (whose getStateForPlacement is authoritative: stairs, doors, gates and beds face the
     * player's way, StairBlock.java:170, DoorBlock.java:112, FenceGateBlock.java:116, BedBlock.java:178; observers face
     * the look direction; a hook faces away from its wall) came out reversed, a click that placed nothing rotated the
     * block that was already there, and whether it ran depended on the stack size (it read the held stack after the
     * place, and the last item leaves it empty). The record names the placed cell, read before FC ran. An FC-placed
     * tripwire hook is never rewritten: FC's onBlockPlaced already faces it away from the clicked wall (Modern-Common
     * BlockTripWireSource, 1.5.2 vanilla/server BlockTripWireSource.java:88-113), and the player's look could turn it
     * onto another solid block beside the target. FC-route stairs stay reversed until decision 6(c) retires the rewrite
     * (FCBlockStairsBase.PreBlockPlacedBy already faces them).</p>
     */
    // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: the click frame closes here, after the FC placement is finished, at every return of useItemOn
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void btw$afterUseItemOn(ServerPlayer serverPlayer, Level world,
                                    ItemStack mcStack, InteractionHand hand,
                                    BlockHitResult hitResult,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(serverPlayer);
        try {
            if (!(world instanceof ServerLevel)) return;
            // FCMOD-INTEGRATION(1.20.1): placement_validity: only a block FC's Stage 2 placed in this click is finished here
            if (fcPlayer.stage2() != PlayerBridge.Stage2.PLACED) return;
            net.minecraft.core.BlockPos placePos = fcPlayer.stage2At();
            if (placePos == null) return;
            net.minecraft.world.level.block.state.BlockState placed = world.getBlockState(placePos);

            // FCMOD-INTEGRATION(1.20.1): placement_validity: an FC-placed tripwire hook keeps the facing FC's onBlockPlaced gave it (Modern-Common BlockTripWireSource, 1.5.2 vanilla/server BlockTripWireSource.java:88-113); decision 6(c) retires the rewrite for the hook
            // In a corner the player's look could turn it onto the other solid block beside the target.
            if (!(placed.getBlock() instanceof net.minecraft.world.level.block.TripWireHookBlock)) {
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
                // C': never to a facing the block cannot stay in (a block hung on a wall, such as a lever or a button,
                // keeps facing away from it: its canSurvive reads the block behind its facing)
                if (!fixed.equals(placed) && fixed.canSurvive(world, placePos)) {
                    world.setBlock(placePos, fixed, 2);
                }
            }

            // FCMOD-INTEGRATION(1.20.1): placement_validity: an FC-placed trapdoor or fence gate against a live signal opens and powers, as 1.20.1's getStateForPlacement does (ruling 4; hook delta G2)
            // FC's ItemBlock sets oak trapdoor 96 and gate 107 closed, where TrapDoorBlock.java:129-131 and
            // FenceGateBlock.java:115-119 open and power them at placement; 1.20.1 redstone only acts on a change of
            // power, so they stayed closed until a neighbour changed. Doors: the iron door places through 1.20.1, the
            // oak door too (ProxyBlock.isPortGapDoorItem).
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
