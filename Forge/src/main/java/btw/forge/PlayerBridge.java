package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

/**
 * Bridges {@link Player} to {@link btw.modern.EntityPlayer}.
 * FC code interacts with players through btw.modern.EntityPlayer fields
 * and methods; this bridge synchronises state in both directions.
 *
 * Works for BOTH ServerPlayer (server-side) and LocalPlayer (client-side)
 * so that FC's mining speed calculations are consistent on both sides.
 *
 * Instances are bound to their Player OBJECT via {@link #getOrCreate(Player)} — see
 * {@link Holder} for why this is not a map keyed by the player.
 */
public class PlayerBridge extends btw.modern.EntityPlayerMP {
    private final Player realPlayer;

    /**
     * Implemented on {@link Player} by {@code PlayerMixin}: each Player object carries its own
     * bridge in a field. This replaces a static {@code WeakHashMap<Player, PlayerBridge>}, which
     * was wrong three ways:
     * <ul>
     *   <li>MC {@code Entity.equals/hashCode} compare the entity id (Entity.java:341-351), and
     *       1.20.1 {@code PlayerList.respawn} (PlayerList.java:440-443: new ServerPlayer,
     *       restoreFrom, setId(old id)) gives the respawned player the dead one's id. After the
     *       first death (or End-exit credits, ServerGamePacketListenerImpl -> respawn(player, true))
     *       every lookup returned the DEAD/removed player's bridge: health 0, death position, max
     *       health penalty, mobs never targeting the live player (triage HAI-1 / reentry M1).
     *       In singleplayer the LocalPlayer shares its ServerPlayer's id, so client-side lookups
     *       got the SERVER bridge and mutated it from the render thread (design_mining B3-CLIENT).</li>
     *   <li>Each value strongly referenced its key ({@link #realPlayer}), so no entry was ever
     *       evicted and every closed world's player/level graph stayed reachable (triage RE2).</li>
     *   <li>WeakHashMap is not thread-safe, but the render thread and the integrated-server
     *       thread both call {@link #getOrCreate(Player)}.</li>
     * </ul>
     * A field on the player is identity-bound by construction, needs no eviction (bridge and
     * player become unreachable together) and gives a respawned/cloned ServerPlayer its own
     * bridge (ServerPlayerMixin.btw$restoreFrom copies the FC state across).
     */
    public interface Holder {
        PlayerBridge btw$getPlayerBridge();

        void btw$setPlayerBridge(PlayerBridge bridge);
    }

    /** Guards bridge creation (double-checked against the volatile Holder field). */
    private static final Object CREATE_LOCK = new Object();

    private final CapabilitiesBridge capBridge;
    private final InventoryBridge invBridge;

    /**
     * Stored by the PlayerMixin attack HEAD injection so downstream code
     * can apply the FC penalty-based melee damage modifier.
     * Reset to 1.0F after each attack cycle.
     *
     * @see btw.modern.EntityPlayer#GetMeleeDamageModifier()
     */
    public float pendingMeleeDamageModifier = 1.0F;

    /**
     * The FC ItemFood whose 1.5.2 onEaten last fed this player (FoodStats.addStats(ItemFood)) and the
     * player tick it did so in; read by {@link #fcFedForEat}. Server thread only.
     */
    private btw.modern.ItemFood lastFcFedFood;
    private int lastFcFedTick;

    /** Set once fcFedForEat failed, so the failure is logged a single time. */
    private static volatile boolean fcFedForEatFailureLogged;

    /**
     * True when FC's own food code already fed {@code eater} for this eat of {@code item}: the FC ItemFood
     * that fed the player in the current tick is {@code item}'s FC item ({@link ProxyItem#fcItemFor}).
     * ItemStackMixin.btw$finishUsing runs that 1.5.2 ItemFood.onEaten at ItemStack.finishUsingItem HEAD, i.e.
     * the nutrition (vanilla/server ItemFood.java:70, FoodStats.addStats with FC's x3 GetHungerRestored) and
     * the onFoodEaten potion effect (:72, :76-82), BEFORE Item.finishUsingItem -> LivingEntity/Player.eat
     * applies vanilla's FoodProperties nutrition (FoodDataMixin) and effects (LivingEntityMixin.btw$addEatEffect)
     * in the same call. Exactly then those must be skipped, so every food restores once: FC's 1.5.2 values
     * for an FC ItemFood (raw meat, melon, carrot, potato, stew, and since FD-R1 the 1.5.2 vanilla ItemFoods:
     * bread, apples, cooked meats, cookie, golden apple ...), vanilla's for everything FC did not feed
     * (modern-only foods, non-player eaters, the client, an eat the FC hook skipped). Recorded at the feed itself rather than predicted from
     * the item, so it cannot disagree with the hook that fed. Never throws.
     */
    public static boolean fcFedForEat(net.minecraft.world.item.Item item,
                                      net.minecraft.world.entity.LivingEntity eater) {
        if (item == null || !(eater instanceof Player player)) return false;
        try {
            PlayerBridge bridge = ((Holder) player).btw$getPlayerBridge();
            return bridge != null && bridge.lastFcFedFood != null
                    && bridge.lastFcFedTick == player.tickCount
                    && ProxyItem.fcItemFor(item) == bridge.lastFcFedFood;
        } catch (Throwable t) {
            if (!fcFedForEatFailureLogged) {
                fcFedForEatFailureLogged = true;
                LOGGER.warn("FC food check failed for {}; vanilla nutrition applies (logged once): {}",
                        item, t.toString(), t);
            }
            return false;
        }
    }

    /**
     * Returns (or creates) the PlayerBridge bound to this exact Player object (identity, never
     * the entity id — see {@link Holder}). Works for ServerPlayer and for client-side
     * LocalPlayer/RemotePlayer; a client player always gets its OWN bridge (never the integrated
     * server's), whose {@code worldObj} is a read-only view of its client level.
     * Thread-safe: the render thread and the server thread may both call this.
     */
    public static PlayerBridge getOrCreate(Player player) {
        Holder holder = (Holder) player;
        PlayerBridge bridge = holder.btw$getPlayerBridge();
        if (bridge == null) {
            synchronized (CREATE_LOCK) {
                bridge = holder.btw$getPlayerBridge();
                if (bridge == null) {
                    bridge = new PlayerBridge(player);
                    holder.btw$setPlayerBridge(bridge);
                }
            }
        }
        bridge.syncFromReal();
        return bridge;
    }

    /**
     * Convenience overload for ServerPlayer.
     */
    public static PlayerBridge getOrCreate(ServerPlayer player) {
        return getOrCreate((Player) player);
    }

    /**
     * The player's bridge as its last sync left it, for FC readers that walk every player each tick (WorldBridge's
     * playerEntities view): ServerPlayerMixin.btw$tick syncs it every server tick. Creates and syncs a missing one.
     */
    static PlayerBridge current(Player player) {
        PlayerBridge bridge = ((Holder) player).btw$getPlayerBridge();
        return bridge != null ? bridge : getOrCreate(player);
    }

    /**
     * NEW, uncached bridge for read-only client-side FC queries (FCMiningSpeed dig prediction).
     * Bypasses {@link #getOrCreate(Player)}: MC Entity.equals/hashCode compare ids (Entity.java:341-351)
     * and the singleplayer LocalPlayer shares its ServerPlayer's id, so the old id-keyed cache
     * returned the SERVER bridge and the render thread would syncFromReal() server state.
     */
    public static PlayerBridge createDetached(Player player) { return new PlayerBridge(player); }

    private PlayerBridge(Player player) {
        super(null); // btw.modern.EntityPlayer(World) constructor
        this.realPlayer = player;
        this.capBridge = new CapabilitiesBridge(player.getAbilities());
        this.invBridge = new InventoryBridge(player.getInventory());
        this.capabilities = this.capBridge;
        this.inventory = this.invBridge;
        this.inventory.player = this; // InventoryBridge passes null to super(); fix the backref
        // Records which FC ItemFood fed the player in which tick, for fcFedForEat: 1.5.2 ItemFood.onEaten
        // feeds through exactly this call (vanilla/server ItemFood.java:70 getFoodStats().addStats(this)).
        this.foodStats = new btw.modern.FoodStats() {
            @Override
            public void addStats(btw.modern.ItemFood food) {
                lastFcFedFood = food;
                lastFcFedTick = realPlayer.tickCount;
                super.addStats(food);
            }
        };
        // 1.5.2 EntityPlayer.username is always the login name (vanilla/server EntityPlayerMP.java:117,
        // set at construction). FC reads it directly, e.g. FCEntityWolf.java:310 owner check
        // (username.equalsIgnoreCase -> NPE on null) and :704 setOwner. Set it for every bridge (server,
        // client, detached, respawned); FCLoginHandler.bindUsername only fills a null, so it stays a no-op.
        this.username = player.getGameProfile().getName();
        // NetServerHandler bridge — translates FC packets to MC 1.20.1 packets
        this.playerNetServerHandler = new ForgeNetServerHandler(player);
        // World bridge — the WorldBridge for a ServerLevel. A client player gets a read-only view
        // of ITS client level (isRemote = true, block id/META reads, no writes — the same world FC
        // client entities use), never null: FC block code dereferences the world (e.g.
        // FCBlockStone.getBlockHardness -> GetStrata -> getBlockMetadata) and used to NPE on
        // dedicated-server clients; in singleplayer it only "worked" because client lookups
        // aliased the server bridge. syncFromReal() never replaces it for a client level.
        // Parity: createDummyClientWorld derives vanilla-id legacy metadata with the server's own
        // WorldBridge.deriveVanillaMetadata (log stump bit, axis, ...), so client dig prediction
        // (FCMiningSpeed; FCBlockLog.getBlockHardness x3 for a stump) matches the server. Only the
        // server-side vanillaMetaOverrides map is invisible here.
        if (player.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
        } else if (player.level() != null && player.level().isClientSide()) {
            try {
                this.worldObj = ProxyEntity.createDummyClientWorld(player.level());
            } catch (Throwable t) {
                LOGGER.warn("Could not create client FC world for {}: {}", player.getName().getString(), t.toString());
            }
        }
        syncFromReal();
    }

    /**
     * Sync state FROM real MC player TO FC player (call before FC tick).
     */
    public void syncFromReal() {
        // EntityPlayerMP.UpdateHealthAndHungerEffects / UpdateGloomState stagger per-player
        // work with (worldTime + entityId) % 80; without this the field is 0 for every player
        // and they all fire on the same tick.
        this.entityId = realPlayer.getId();
        this.posX = realPlayer.getX();
        this.posY = realPlayer.getY();
        this.posZ = realPlayer.getZ();
        // Rebuild bounding box from position — FC's melee attack checks
        // target.boundingBox for Y overlap (EntityMob line 185).
        this.width = realPlayer.getBbWidth();
        this.height = realPlayer.getBbHeight();
        this.setPosition(this.posX, this.posY, this.posZ);
        this.motionX = realPlayer.getDeltaMovement().x;
        this.motionY = realPlayer.getDeltaMovement().y;
        this.motionZ = realPlayer.getDeltaMovement().z;
        this.rotationYaw = realPlayer.getYRot();
        this.rotationPitch = realPlayer.getXRot();
        // CF2-MX3: a rider's updateRidden adds the mount's turn since last tick (vanilla/client Entity.java:1800-1851); without
        // the previous rotation a head crab spun 10 degrees a tick
        this.prevRotationYaw = realPlayer.yRotO;
        this.prevRotationPitch = realPlayer.xRotO;
        this.onGround = realPlayer.onGround();
        // FCMOD-INTEGRATION(1.20.1): placement_validity: the inWater FIELD, which FC reads directly: 4.B's mid-air
        // rule (Modern-Common EntityPlayer.canPlayerEdit; vanilla/server EntityPlayer.java:2037) lets a swimming player
        // place, and FCEntitySquid targets a player by it (Common FCEntitySquid.java:424, :837). 1.5.2 set it every
        // tick in Entity.handleWaterMovement, which never runs for the bridge; isInWater() already answers from 1.20.1.
        this.inWater = realPlayer.isInWater();
        this.health = fcHealth(realPlayer.getHealth());
        this.fallDistance = realPlayer.fallDistance;
        this.ticksExisted = realPlayer.tickCount;
        this.experienceLevel = realPlayer.experienceLevel;
        this.capBridge.sync();
        this.invBridge.sync();
        if (realPlayer.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
            // Keep difficulty setting current (FC checks it for food/starvation)
            this.worldObj.difficultySetting = sl.getDifficulty().getId();
            // CF2-MX3: 1.5.2 World.updateEntityWithOptionalForce's mount cleanup (vanilla/server World.java:1972-1982), which no FC
            // world update runs for the bridge: a head crab that died, left the world or let go stops riding the player, and with it
            // the 4.B head-crab weight (EntityLiving.isWeighted)
            btw.modern.Entity rider = this.riddenByEntity;
            if (rider != null && (rider.ridingEntity != this || !FcRiding.isLive(rider, sl))) {
                if (rider.ridingEntity == this) rider.ridingEntity = null;
                this.riddenByEntity = null;
            }
        }
    }

    /**
     * Write FC inventory changes back to the real MC inventory.
     * Call after FC code that modifies items (stackSize, damage, etc.).
     */
    public void syncInventoryToReal() {
        invBridge.writeBackAll();
    }

    /**
     * INV-RESYNC: drops the FC inventory edits not yet written to the real inventory
     * ({@link InventoryBridge#discardPendingEdits()}). For an FC call that threw before its write-back: the next sync
     * dropped such edits before INV-RESYNC, and would now write them.
     */
    public void discardInventoryEdits() {
        invBridge.discardPendingEdits();
    }

    /**
     * FC's integer view of a 1.20.1 float health: truncated as before, except that a living player below one point
     * reads as 1, never as 0. FC treats health 0 as dead (the frozen EntityLiving.isEntityAlive needs health &gt; 0, and
     * FoodStats heals only above 0), and {@link #syncToReal} would write that 0 back and kill a player at 0.4 HP.
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: other mods' fractional damage and healing survive the FC sync
     * (INTEGRATIONS #021).
     */
    private static int fcHealth(float health) {
        return health > 0F && health < 1F ? 1 : (int) health;
    }

    /**
     * Sync state FROM FC player TO real MC player (call after FC tick).
     */
    public void syncToReal() {
        // FCMOD-INTEGRATION(1.20.1): hunger_contract: write health back only when FC changed it, so 7.5 HP is not truncated
        // to 7 every tick and a player at 0.4 HP (FC 1) is not killed or healed by the sync itself
        if (this.health != fcHealth(realPlayer.getHealth())) {
            realPlayer.setHealth((float) this.health);
        }
        // Sync velocity if FC modified it (knockback, fling)
        if (this.motionX != realPlayer.getDeltaMovement().x ||
            this.motionY != realPlayer.getDeltaMovement().y ||
            this.motionZ != realPlayer.getDeltaMovement().z) {
            realPlayer.setDeltaMovement(this.motionX, this.motionY, this.motionZ);
        }
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): head_crab: the end of 4.B's tentacle fling (FCEntitySquid.java:1192-1280). 1.5.2 sent the new
     * velocity to the player through velocityChanged (setBeenAttacked, :1275; vanilla/server EntityTrackerEntry.java:287-290);
     * here the next syncFromReal (:207-209) would overwrite the bridge's motion, so it is put on the real player with
     * hurtMarked, which 1.20.1 sends as it sends knockback (ServerEntity.sendChanges). The disarm branch (a 4.B block,
     * isBlocking) emptied the FC stack: the inventory is synced at once (INV-RESYNC writes it through while the real slot is
     * unchanged), so the thrown copy and the emptied hand land in the same call.
     */
    @Override
    public void OnFlungBySquidTentacle(btw.modern.Entity squid) {
        super.OnFlungBySquidTentacle(squid);   // frozen EntityLiving: the hurt sound (vanilla/server EntityLiving.java:3458-3463)
        if (realPlayer instanceof ServerPlayer) {
            invBridge.sync();
            leaveVehicleForFling();            // 4.B unseated the target before either branch (FCEntitySquid.java:1197-1201)
            if (this.velocityChanged) landFcMotion();
        }
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): head_crab: the same gap for 4.B's cow kick (Client FCEntityCow.java:541-553 ->
     * Entity.OnKickedByCow -> FlingAwayFromEntity, vanilla/client Entity.java:2558-2601), which flings the bridge only. The
     * fling starts from the real velocity after the kick's hit (1.20.1 knockback), as 4.B's started from the velocity its
     * attackEntityFrom left.
     */
    @Override
    public void OnKickedByCow(btw.modern.Entity cow) {
        if (!(realPlayer instanceof ServerPlayer)) {
            super.OnKickedByCow(cow);
            return;
        }
        net.minecraft.world.phys.Vec3 v = realPlayer.getDeltaMovement();
        this.motionX = v.x;
        this.motionY = v.y;
        this.motionZ = v.z;
        super.OnKickedByCow(cow);
        leaveVehicleForFling();
        landFcMotion();
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): head_crab: 4.B unseats the target of an FC fling first (FCEntitySquid.java:1197-1201;
     * Entity.FlingAwayFromEntity, vanilla/client Entity.java:2570-2573) through its ridingEntity, which the bridge never
     * sets (the vehicle is 1.20.1's, see canPlayerEdit). The same on the real vehicle, so a tentacle or a cow's kick
     * throws the player out of a boat, minecart or saddle, as FcRiding's head-crab rule does. Server only.
     */
    private void leaveVehicleForFling() {
        if (realPlayer.isPassenger()) realPlayer.stopRiding();
    }

    /**
     * Puts the bridge's motion on the real player and marks it for the motion packet (hurtMarked). Server only. A
     * spectator is never moved: 1.20.1 pushes and knocks back no spectator, while FC's tentacle tip still finds its
     * bridge (WorldBridge.getEntitiesWithinAABB).
     */
    private void landFcMotion() {
        this.velocityChanged = false;
        if (realPlayer.isSpectator()) return;
        realPlayer.setDeltaMovement(this.motionX, this.motionY, this.motionZ);
        realPlayer.hurtMarked = true;
    }

    // ================================================================
    // Per-click frame: one ServerPlayerGameMode.useItemOn (fc_item_use_order)
    // ================================================================

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_item_use_order
     *
     * <p>What the hooks of one right-click on a block share. 1.5.2 ran the click as one call,
     * ItemInWorldManager.activateBlockOrUseItem (vanilla/server ItemInWorldManager.java:376-407): the block's
     * onBlockActivated, else the held item's tryPlaceItemIntoWorld. In 1.20.1 the same click is spread over
     * ServerPlayerGameMode.useItemOn (ServerPlayerGameMode.java:317-370): the RightClickBlock event (:323), block use
     * (:343, where ProxyBlock.use and BlockBehaviorMixin.btw$use run FC Stage 1 and Stage 2) and the item step
     * (ItemStack.useOn, :355/:358, where ServerPlayerGameModeMixin.btw$fcItemStep runs FC for blocks without Stage 2).
     * The frame carries, per click:</p>
     * <ul>
     *   <li>{@code fcUseDenied}: the event's final {@code useItem == DENY} (FcItemUseOrder, LOWEST), which Stage 2 cannot
     *       read itself. 1.20.1 stops the item step on it (:351); ProxyBlock.tryPlaceHeldItem stops FC on it.</li>
     *   <li>{@code fcUseRanAt}: the position whose FC onItemUse already ran in this click (Stage 2), so the item step
     *       never runs it a second time there. Keyed by position, so a foreign {@code blockstate.use} nested in the
     *       click cannot suppress the item step at another one.</li>
     *   <li>{@code stage2} (placement_validity): what FC's placement did in this click when the main-hand item is a
     *       1.20.1 BlockItem whose FC item is of the 4.B ItemBlock family (ProxyBlock.tryPlaceHeldItem): PLACED, with
     *       the cell it placed the held block in ({@code stage2At}), or REFUSED (only for a player who may build; an
     *       adventure-mode refusal is left to 1.20.1's CanPlaceOn rule). FC's refusal is 4.B's final verdict there, so
     *       the item step refuses 1.20.1's BlockItem fallback on a REFUSED recorded at the clicked block
     *       ({@code fcUseRanAt}; 4.B's client never sent a placement FC refused, vanilla/client
     *       PlayerControllerMP.java:425-433), and the RETURN inject finishes a PLACED block (facing guard, redstone
     *       state). NONE otherwise, and outside any useItemOn.</li>
     * </ul>
     *
     * <p>ServerPlayerGameModeMixin pushes a frame as the first statement of useItemOn and pops it in its RETURN inject,
     * and also before each of its own cancels (a RETURN inject does not run after an injector cancels). Nested useItemOn
     * calls in one tick stack. A frame leaked by another mod's cancel or by a throw is dropped by the next
     * {@link #beginUseClick} of a later server tick. Outside any useItemOn (a third-party caller that posts
     * RightClickBlock and calls {@code blockstate.use} itself, as deployer-style fake players do), the denial is kept
     * loose on the bridge and every post overwrites it; nothing else is recorded. Server thread only.</p>
     */
    private static final class UseClick {
        /** {@code MinecraftServer.getTickCount()} when the click began. */
        final long tick;
        boolean fcUseDenied;
        net.minecraft.core.BlockPos fcUseRanAt;
        Stage2 stage2 = Stage2.NONE;
        /** PLACED only: the cell FC placed the held block in; null when it could not be told. */
        net.minecraft.core.BlockPos stage2At;

        UseClick(long tick) {
            this.tick = tick;
        }
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): placement_validity: the outcome of FC's placement in one click, for a main-hand 1.20.1
     * BlockItem whose FC item is of the 4.B ItemBlock family (see {@link UseClick}).
     */
    public enum Stage2 {
        /** No FC ItemBlock-family placement ran in this click (or no click is open). */
        NONE,
        /** FC placed the held block (4.B onItemUse returned true). */
        PLACED,
        /** FC refused it (false, or a throw) for a player who may build: final, 1.20.1's BlockItem must not place it instead. */
        REFUSED
    }

    /** The open clicks, innermost last. */
    private final java.util.ArrayDeque<UseClick> useClicks = new java.util.ArrayDeque<>();

    /** The RightClickBlock denial posted outside any useItemOn (see {@link UseClick}). */
    private boolean looseFcUseDenied;

    /** The innermost open click, or null outside useItemOn. */
    private UseClick currentUseClick() {
        return useClicks.peekLast();
    }

    /**
     * Opens the frame of one useItemOn call made in server tick {@code tick}. Frames of any other tick are leaks (a
     * foreign cancel or a throw skipped their pop) and are dropped first.
     */
    public void beginUseClick(long tick) {
        useClicks.removeIf(c -> c.tick != tick);
        useClicks.addLast(new UseClick(tick));
    }

    /** Closes the innermost click frame; nothing when none is open. */
    public void endUseClick() {
        useClicks.pollLast();
    }

    /** True when this click's RightClickBlock denied the item ({@code useItem == DENY}). */
    public boolean isFcUseDenied() {
        UseClick c = currentUseClick();
        return c != null ? c.fcUseDenied : looseFcUseDenied;
    }

    /** Records the RightClickBlock {@code useItem} verdict of this click (every post overwrites it). */
    public void setFcUseDenied(boolean denied) {
        UseClick c = currentUseClick();
        if (c != null) {
            c.fcUseDenied = denied;
        } else {
            looseFcUseDenied = denied;
        }
    }

    /** Records that FC onItemUse ran at {@code pos} in this click; nothing outside useItemOn. */
    public void markFcItemUse(net.minecraft.core.BlockPos pos) {
        UseClick c = currentUseClick();
        if (c != null) {
            c.fcUseRanAt = pos.immutable();
        }
    }

    /** True when FC onItemUse already ran at {@code pos} in this click. */
    public boolean fcItemUseRanAt(net.minecraft.core.BlockPos pos) {
        UseClick c = currentUseClick();
        return c != null && pos.equals(c.fcUseRanAt);
    }

    /** This click's FC placement outcome; NONE outside useItemOn. */
    public Stage2 stage2() {
        UseClick c = currentUseClick();
        return c != null ? c.stage2 : Stage2.NONE;
    }

    /** Records this click's FC placement outcome, with no cell; nothing outside useItemOn. */
    public void setStage2(Stage2 stage2) {
        setStage2(stage2, null);
    }

    /** Records this click's FC placement outcome and, for PLACED, the cell FC placed in; nothing outside useItemOn. */
    public void setStage2(Stage2 stage2, net.minecraft.core.BlockPos at) {
        UseClick c = currentUseClick();
        if (c != null) {
            c.stage2 = stage2 != null ? stage2 : Stage2.NONE;
            c.stage2At = c.stage2 == Stage2.PLACED && at != null ? at.immutable() : null;
        }
    }

    /** PLACED only: the cell this click's FC placement put the held block in; null otherwise or when unknown. */
    public net.minecraft.core.BlockPos stage2At() {
        UseClick c = currentUseClick();
        return c != null && c.stage2 == Stage2.PLACED ? c.stage2At : null;
    }

    // ================================================================
    // Penalty level sync: FC originally used DataWatcher entries (IDs 22-31)
    // to sync penalty levels to the client. In the Forge 1.20.1 port, this
    // is replaced by a custom network packet (BTWNetwork.PenaltySync) sent
    // from ServerPlayerMixin.btw$tick() after UpdateModStatusVariables().
    // Client-side values are stored in BTWNetwork.clientGloomLevel etc.
    // and rendered by btw.forge.client.BTWHudOverlay.
    // ================================================================

    // ================================================================
    // Override methods that need to reach the real player
    // ================================================================

    @Override
    public boolean isSprinting() {
        return realPlayer.isSprinting();
    }

    @Override
    public boolean isSneaking() {
        return realPlayer.isCrouching();
    }

    @Override
    public int getMaxHealth() {
        // Called virtually by vanilla 1.5.2 EntityLiving.<init>, which runs
        // BEFORE our constructor assigns {@code realPlayer}. Return the
        // vanilla default in that case; the actual value is refreshed by
        // {@link #syncFromReal()} once construction completes.
        return realPlayer == null ? 20 : (int) realPlayer.getMaxHealth();
    }

    /**
     * Returns the underlying Forge Player.
     */
    public Player getRealPlayer() {
        return realPlayer;
    }

    /**
     * Returns the underlying player as ServerPlayer, or null if client-side.
     */
    public ServerPlayer getServerPlayer() {
        return realPlayer instanceof ServerPlayer sp ? sp : null;
    }

    // ================================================================
    // Bridge event hooks — delegate FC methods to real MC player
    // ================================================================

    /**
     * Sends a system chat message to the player.
     * FC code calls this for gameplay messages (gloom warnings, etc.).
     */
    @Override
    public void addChatMessage(String msg) {
        if (realPlayer instanceof ServerPlayer sp) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    /**
     * Sends a raw chat message via the player's network connection.
     * Used by FC for system-level messages that bypass normal chat formatting.
     */
    @Override
    public void AddRawChatMessage(String msg) {
        if (realPlayer instanceof ServerPlayer sp) {
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        }
    }

    /**
     * Heals the player by the given amount.
     * FC uses int health; MC 1.20.1 uses float — cast at boundary.
     */
    @Override
    public void heal(int amount) {
        realPlayer.heal((float) amount);
        // Re-read health so syncToReal() doesn't overwrite the heal
        this.health = fcHealth(realPlayer.getHealth());
    }

    /**
     * FC calls this for starvation damage, fire damage, FC mob melee, FC projectiles, etc.
     * Delegates to the real MC player's hurt() method. The source goes through
     * {@link WorldBridge#toMcDamageSource}: an FC arrow / thrown item / fireball becomes the 1.20.1
     * projectile type with the shooter as the causing entity, so the kill is credited to the shooter
     * ("shot by" death message), shields and projectile protection apply, and PvP rules see the shooter.
     * 1.5.2 never difficulty-scaled such indirect damage -- EntityDamageSource.isDifficultyScaled tests the
     * DIRECT entity, the projectile (vanilla/server EntityDamageSource.java:45-48, EntityPlayer.java:942-958)
     * -- while 1.20.1 Player.hurt scales any source whose CAUSING entity is a non-player LivingEntity
     * (DamageSource.scalesWithDifficulty), so an FC skeleton's arrow would hit x1.5 on Hard; see
     * {@link DamageSourceMapping#withoutDifficultyScaling}. Melee ("mob") stays scaled, as in 1.5.2.
     */
    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource source, int amount) {
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.minecraft.world.damagesource.DamageSource mcSource =
                    WorldBridge.toMcDamageSource(source, sp.serverLevel());
            if (source instanceof btw.modern.EntityDamageSourceIndirect) {
                mcSource = DamageSourceMapping.withoutDifficultyScaling(mcSource);
            }
            boolean result = realPlayer.hurt(mcSource, (float) amount);
            this.health = fcHealth(realPlayer.getHealth());
            return result;
        }
        return false;
    }

    /**
     * Grants experience points to the player.
     * FC calls this for XP awards from block harvesting, mob kills, etc.
     */
    @Override
    public void addExperience(int amount) {
        realPlayer.giveExperiencePoints(amount);
    }

    @Override
    public void addExperienceLevel(int levels) {
        realPlayer.giveExperienceLevels(levels);
        this.experienceLevel = realPlayer.experienceLevel;
    }

    /**
     * 1.5.2 EntityLiving.onItemPickup bridge — the frozen EntityXPOrb.onCollideWithPlayer
     * calls this to fire the collect animation when an XP orb is absorbed. Resolves the
     * FC entity back to its live proxy via entityId (ProxyEntity.tick syncs it) and
     * delegates to ServerPlayer.take(), which broadcasts the modern take-item packet.
     */
    @Override
    public void onItemPickup(btw.modern.Entity entity, int count) {
        if (entity != null && realPlayer instanceof ServerPlayer sp) {
            net.minecraft.world.entity.Entity mcEntity = sp.serverLevel().getEntity(entity.entityId);
            if (mcEntity != null) {
                sp.take(mcEntity, count);
            }
        }
    }

    /**
     * 1.5.2 EntityLiving.onDeath bridge — FC kills players directly in a few places
     * (e.g. EntityPlayer.DetonateCarriedBlastingOil sets health = 0 then calls
     * onDeath(DamageSource.generic)). Delegates to the real ServerPlayer death path so
     * the death message, stats and respawn screen fire instead of the frozen
     * EntityLiving death logic running against the bridge object.
     */
    @Override
    public void onDeath(btw.modern.DamageSource source) {
        if (realPlayer instanceof ServerPlayer sp) {
            sp.setHealth(0.0F);
            sp.die(DamageSourceMapping.getModern(source, sp.serverLevel()));
            this.health = 0;
        }
    }

    /**
     * Legacy method name for dropPlayerItem.
     * FC's Container.slotClick() calls this when dropping items outside
     * the inventory window (clicking outside with a held stack).
     */
    @Override
    public void func_71012_a(btw.modern.ItemStack stack) {
        dropPlayerItem(stack);
    }

    /**
     * Drops an item from the player's position with a small random velocity.
     * Converts FC ItemStack to MC ItemStack via {@link ItemStackHelper} and
     * delegates to the real player's drop method.
     *
     * Returns an FC EntityItem wrapper around the spawned MC ItemEntity.
     * Most FC callers ignore the return value, but some use it to customize
     * the dropped item (e.g., setting no-despawn or adjusting pickup delay).
     */
    @Override
    public btw.modern.EntityItem dropPlayerItem(btw.modern.ItemStack stack) {
        if (stack == null) return null;
        net.minecraft.world.item.ItemStack mcStack = ItemStackHelper.toMcStack(stack);
        if (mcStack.isEmpty()) return null;
        net.minecraft.world.entity.item.ItemEntity mcItem = realPlayer.drop(mcStack, false);
        if (mcItem == null) return null;
        // Wrap the returned MC ItemEntity in an FC EntityItem so callers
        // can manipulate pickup delay, despawn time, etc.
        btw.modern.EntityItem fcItem = new btw.modern.EntityItem(
                this.worldObj,
                mcItem.getX(), mcItem.getY(), mcItem.getZ(),
                stack);
        fcItem.entityId = mcItem.getId();
        return fcItem;
    }

    /**
     * Checks if a potion effect is currently active on the player.
     * Translates FC Potion to MC MobEffect via {@link PotionMapping}.
     */
    @Override
    public boolean isPotionActive(btw.modern.Potion potion) {
        MobEffect effect = PotionMapping.getEffect(potion);
        return effect != null && realPlayer.hasEffect(effect);
    }

    /**
     * Checks if a potion effect is currently active by legacy ID.
     * Translates FC potion ID to MC MobEffect via {@link PotionMapping}.
     */
    @Override
    public boolean isPotionActive(int potionId) {
        MobEffect effect = PotionMapping.getEffect(potionId);
        return effect != null && realPlayer.hasEffect(effect);
    }

    /**
     * Returns the active potion effect for the given FC Potion, or null.
     * Wraps the MC effect instance back into an FC PotionEffect.
     */
    @Override
    public btw.modern.PotionEffect getActivePotionEffect(btw.modern.Potion potion) {
        MobEffect effect = PotionMapping.getEffect(potion);
        if (effect == null) return null;
        net.minecraft.world.effect.MobEffectInstance instance = realPlayer.getEffect(effect);
        if (instance == null) return null;
        return new btw.modern.PotionEffect(potion.id, instance.getDuration(), instance.getAmplifier());
    }

    /**
     * Adds a potion effect to the player.
     * Translates FC PotionEffect to MC MobEffectInstance via {@link PotionMapping}.
     */
    @Override
    public void addPotionEffect(btw.modern.PotionEffect effect) {
        MobEffect mobEffect = PotionMapping.getEffect(effect.getPotionID());
        if (mobEffect != null) {
            // FCMOD-INTEGRATION(1.20.1): fc_beacon: FC's ambient flag reaches 1.20.1 (4.B: beacon, mill stone, starvation, near-death and gloom effects are ambient)
            realPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    mobEffect, effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), true));
        }
    }

    /**
     * Removes a potion effect from the player by legacy FC potion ID.
     */
    @Override
    public void removePotionEffect(int potionId) {
        MobEffect effect = PotionMapping.getEffect(potionId);
        if (effect != null) {
            realPlayer.removeEffect(effect);
        }
    }

    /**
     * Returns whether the player is currently sleeping: the real player's 1.20.1 state. Beds sleep (maintainer ruling 3,
     * 2026-10-19: 1.20.1's sleep, messages and night skip, while a bed never sets the spawn, BedSpawnPolicy), so this is
     * true while the player lies in a bed, as 1.5.2 EntityPlayer.isPlayerSleeping was. 4.B removed sleeping (its
     * FCBlockBed swallowed the click), so FC itself never put a player to sleep.
     */
    @Override
    public boolean isPlayerSleeping() {
        return realPlayer.isSleeping();
    }

    /**
     * Returns whether the player is in creative mode.
     * Delegates to the real player's abilities.
     */
    @Override
    public boolean isInCreativeMode() {
        return realPlayer.getAbilities().instabuild;
    }

    /**
     * Returns the player's food level from the FC food stats system.
     * FC food level ranges 0-60 (3x vanilla resolution).
     */
    @Override
    public int getFoodLevel() {
        return foodStats.getFoodLevel();
    }

    /**
     * Returns the player's username.
     */
    @Override
    public String getCommandSenderName() {
        return realPlayer.getName().getString();
    }

    // ================================================================
    // FC Spawn System — setSpawnChunk / getBedLocation / isSpawnForced
    // ================================================================

    // FCMOD-INTEGRATION(1.20.1): hardcore_respawn: 4.B's spawnChunk is 1.20.1's respawn point; m_HardcoreSpawnChunk is only the hardcore home

    /**
     * The respawn point FC itself set (FCBlockBeacon.onBlockActivated, Server FCBlockBeacon.java:93): 4.B's beacon bind,
     * with its level. {@link HardcoreRespawn} validates a non-forced respawn point that equals it with 4.B's rules
     * (vanilla/server EntityPlayer.java:2806-2969); any other point is 1.20.1's. Never the hardcore home
     * ({@link #m_HardcoreSpawnChunk}). Copied by ServerPlayerMixin.btw$restoreFrom and saved as btwFcSpawnBind
     * (HardcoreRespawn.readBind/writeBind). Server thread only.
     */
    public net.minecraft.core.BlockPos fcBindPos;
    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> fcBindDim;

    /** 1.5.2 EntityPlayer.setSpawnChunk with two arguments resets the dimension to 0 (vanilla/server EntityPlayer.java:1624-1639). */
    @Override
    public void setSpawnChunk(btw.modern.ChunkCoordinates pos, boolean forced) {
        setSpawnChunk(pos, forced, 0);
    }

    /**
     * 4.B's spawnChunk (vanilla/server EntityPlayer.java:2972-2987) is 1.20.1's respawn point: one slot, last write wins,
     * in both games. FC's only caller is the beacon bind (non-forced, the beacon cell, the beacon's world), which is
     * remembered as {@link #fcBindPos}/{@link #fcBindDim} when 1.20.1 accepted the point (PlayerSetSpawnEvent may veto it).
     * The legacy dimension is resolved against the player's level ({@link HardcoreRespawn#levelForLegacyDim}). The
     * hardcore home is never written here.
     */
    @Override
    public void setSpawnChunk(btw.modern.ChunkCoordinates pos, boolean forced, int dimensionId) {
        this.m_iSpawnDimension = pos != null ? dimensionId : 0;                // fcSpawnDimension mirror (EP:2972-2987)
        if (!(realPlayer instanceof ServerPlayer sp)) return;
        if (pos == null) {
            sp.setRespawnPosition(net.minecraft.world.level.Level.OVERWORLD, null, 0F, false, false);
            fcBindPos = null;
            fcBindDim = null;
            return;
        }
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = HardcoreRespawn.levelForLegacyDim(sp, dimensionId);
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(pos.posX, pos.posY, pos.posZ);
        sp.setRespawnPosition(dim, p, 0F, forced, false);                      // PlayerSetSpawnEvent may veto it
        boolean set = p.equals(sp.getRespawnPosition()) && dim.equals(sp.getRespawnDimension());
        fcBindPos = set && !forced ? p : null;
        fcBindDim = set && !forced ? dim : null;
    }

    /** 4.B's spawnChunk: 1.20.1's respawn point, or null. */
    @Override
    public btw.modern.ChunkCoordinates getBedLocation() {
        net.minecraft.core.BlockPos p = realPlayer instanceof ServerPlayer sp ? sp.getRespawnPosition() : null;
        return p == null ? null : new btw.modern.ChunkCoordinates(p.getX(), p.getY(), p.getZ());
    }

    /** 4.B's spawnForced: 1.20.1's respawn point is forced (/spawnpoint). */
    @Override
    public boolean isSpawnForced() {
        return realPlayer instanceof ServerPlayer sp && sp.isRespawnForced();
    }

    // ================================================================
    // Entity / EntityLiving overrides that delegate to real player
    // ================================================================

    /**
     * Checks if the player is inside the given material (water or lava).
     * FC uses this for mining speed penalties (underwater mining) and
     * movement checks.
     */
    @Override
    public boolean isInsideOfMaterial(btw.modern.Material material) {
        if (material == btw.modern.Material.water) return realPlayer.isInWater();
        if (material == btw.modern.Material.lava) return realPlayer.isInLava();
        return false;
    }

    /**
     * Returns the currently equipped item from the inventory bridge.
     * FC calls this during mining speed calculations and tool checks.
     */
    @Override
    public btw.modern.ItemStack getCurrentEquippedItem() {
        if (inventory != null) return inventory.getCurrentItem();
        return null;
    }

    /**
     * Returns whether the player is in water.
     * Delegates to the real MC player.
     */
    @Override
    public boolean isInWater() {
        return realPlayer.isInWater();
    }

    /**
     * Returns whether the player is in lava.
     * FC calls handleLavaMovement() to check lava contact for
     * damage and movement restrictions.
     */
    @Override
    public boolean handleLavaMovement() {
        return realPlayer.isInLava();
    }

    /**
     * Returns whether the player is on a ladder (or climbable block).
     * FC uses this for movement modifier calculations and
     * fall distance resets.
     */
    @Override
    public boolean isOnLadder() {
        return realPlayer.onClimbable();
    }

    /**
     * 4.B's edit rule, Modern-Common EntityPlayer.canPlayerEdit (vanilla/server EntityPlayer.java:2034-2040): no
     * placement in mid-air unless creative, on the ground, in water, on a ladder, riding or in lava. Its riding clause
     * reads the {@code ridingEntity} field, which the bridge never sets: the vehicle is a 1.20.1 entity that 1.20.1
     * mounts, and an FC view of it in that field would let FC code dismount or re-mount it (FCEntitySquid.java:956-963).
     * So a riding player is asked here, from the real player; every other clause stays Modern-Common's (inWater is
     * synced, the ladder and lava clauses are overridden above).
     * FCMOD-INTEGRATION(1.20.1): placement_validity: FC's refusal of a placement is final for the 4.B ItemBlock family
     * (ServerPlayerGameModeMixin.btw$fcItemStep), so a player in a boat or on a horse must pass the rule as in 4.B
     */
    @Override
    public boolean canPlayerEdit(int x, int y, int z, int side, btw.modern.ItemStack stack) {
        if (realPlayer.isPassenger()) {
            return this.capabilities != null && this.capabilities.allowEdit;
        }
        return super.canPlayerEdit(x, y, z, side, stack);
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_entity_views: FC mounting this entity on a 1.20.1 vehicle it found (a view of
     * VanillaEntityViews: the block dispenser's {@code riddenByEntity.mountEntity(cart)} before it takes a cart, Client
     * FCBlockBlockDispenser.java:292-297) rides or leaves the vehicle itself: leaving it when already on it (4.B
     * EntityPlayer.mountEntity's toggle), else mounting it. The view is never stored in ridingEntity (the vehicle is
     * 1.20.1's; see PlayerBridge.canPlayerEdit). Any other mount is FC's own, unchanged.
     */
    @Override
    public void mountEntity(btw.modern.Entity target) {
        net.minecraft.world.entity.Entity vehicle = VanillaEntityViews.realOf(target);
        if (vehicle == null) {
            super.mountEntity(target);
            return;
        }
        net.minecraft.world.entity.Entity self = realPlayer;
        if (self == null) return;
        if (self.getVehicle() == vehicle) {
            self.stopRiding();
        } else {
            self.startRiding(vehicle);
        }
    }

    /**
     * CF2-MX3: 4.B's latch rule (Modern-Common EntityPlayer.GetCanBeHeadCrabbed) on the real player: only a player 1.20.1
     * lets mobs target (FcTargetRule.isTargetable, the rule that drops a head crab every tick: an Invulnerable-tagged
     * survival player would otherwise be latched and dropped each tick), and the riding clause asked of the real player,
     * since the bridge's ridingEntity is never set (see canPlayerEdit). Server only: FC latches only there
     * (FCEntitySquid.updateEntityActionState, Common :321-328).
     */
    @Override
    public boolean GetCanBeHeadCrabbed(boolean bSquidInWater) {
        ServerPlayer sp = getServerPlayer();
        return sp != null && super.GetCanBeHeadCrabbed(bSquidInWater) && FcTargetRule.isTargetable(sp)
                && !sp.isPassenger() && !sp.isVehicle();
    }

    /**
     * CF2-MX3: where a head-crab squid sits (Entity.updateRiderPosition: posY + getMountedYOffset() + rider.getYOffset()).
     * 4.B returned height * 0.025 from a client posY at eye level (vanilla/client EntityPlayer.java:3215-3219), so its squid
     * sat on the eyes; the bridge's posY is the feet, and the server position is what clients draw.
     */
    @Override
    public double getMountedYOffset() {
        return realPlayer == null ? super.getMountedYOffset() : realPlayer.getEyeHeight() + this.height * 0.025D;
    }

    /**
     * Adds exhaustion to the FC food stats under 1.5.2's own rule, Modern-Common EntityPlayer.addExhaustion
     * (vanilla/server EntityPlayer.java:1934-1946): nothing while {@code disableDamage} (1.20.1 abilities.invulnerable,
     * CapabilitiesBridge, refreshed by every getOrCreate), nothing on a client bridge, and multiplied by the worn-armour
     * weight. Every hunger cost of the server player arrives here: FC's own (jump, harvest, swim-up), vanilla's
     * Player.causeFoodExhaustion and other mods' FoodData.addExhaustion (PlayerMixin, FoodDataMixin).
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: one FC rule for every hunger cost; it used to skip the armour weight
     * and the invulnerable exemption (INTEGRATIONS #021).
     */
    @Override
    public void addExhaustion(float amount) {
        if (this.worldObj == null) return;
        super.addExhaustion(amount);
    }

    /**
     * FoodStats.onUpdate heals only while this is true: the 1.20.1 naturalRegeneration gamerule of the player's level
     * (WorldGamerulesHandler leaves it at its default, true), so the rule, and mods that set it, gate FC's 600-tick heal
     * as they gate vanilla's.
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: 1.5.2 had no such gamerule; FC's heal ignored it (INTEGRATIONS #021).
     */
    @Override
    public boolean IsNaturalRegenerationEnabled() {
        return !(realPlayer.level() instanceof ServerLevel sl)
                || sl.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION);
    }

    // ================================================================
    // Enchantment bridge — query MC 1.20.1 enchantments for FC code
    // ================================================================

    @Override
    public int getEfficiencyEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY, held);
    }

    @Override
    public int getRespirationEnchantLevel() {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.RESPIRATION, realPlayer);
    }

    @Override
    public int getKnockbackEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, held);
    }

    @Override
    public int getFireAspectEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, held);
    }

    @Override
    public int getLootingEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING, held);
    }

    @Override
    public int getUnbreakingEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, held);
    }

    @Override
    public int getFortuneEnchantLevel() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE, held);
    }

    @Override
    public boolean getAquaAffinityEnchant() {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY, realPlayer) > 0;
    }

    @Override
    public boolean getSilkTouchEnchant() {
        net.minecraft.world.item.ItemStack held = realPlayer.getMainHandItem();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH, held) > 0;
    }

    // ================================================================
    // Generalized side-effect bridges — FC code calls these to produce
    // effects in the MC engine. Every method here delegates to the real
    // MC player so FC side effects are visible.
    // ================================================================

    /**
     * FC calls this to start the "using item" animation (bow draw, eating, fire starting).
     * Tells the real MC player to start using their held item, which triggers
     * the animation, use-duration timer, and eventually finishUsingItem/releaseUsing.
     */
    @Override
    public void setItemInUse(btw.modern.ItemStack stack, int duration) {
        super.setItemInUse(stack, duration);
        // Tell the real MC player to start using their held item
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            // MC 1.20.1: startUsingItem triggers the use animation + timer
            sp.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): head_crab: 4.B EntityPlayer.isBlocking (vanilla/client EntityPlayer.java:216-219) is "using
     * an item whose use action is block". The bridge's own use fields are never cleared (Modern-Common EntityPlayer.java:61-66,
     * :136-153: no clearItemInUse caller), so after one right-click with a BTW sword or refined tool the bridge "blocked"
     * forever, and the squid's tentacle, the one FC reader (FCEntitySquid.java:1229-1259), would throw a copy of whatever the
     * player holds. Asked of the real player: using, in the main hand (FC empties mainInventory[currentItem]), a BTW item with
     * the block use action (ProxyItem.getUseAnimation, ProxyItem.java:564-574). A 1.20.1 shield is not 4.B blocking: FC's
     * copy keeps only id, count, damage and enchantments. On yes the inventory view is re-synced first, so the stack FC
     * empties is the one in the hand now.
     */
    @Override
    public boolean isBlocking() {
        if (!(realPlayer instanceof ServerPlayer sp) || !sp.isUsingItem()
                || sp.getUsedItemHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return false;
        }
        net.minecraft.world.item.ItemStack use = sp.getUseItem();
        if (!(use.getItem() instanceof ProxyItem) || use.getUseAnimation() != net.minecraft.world.item.UseAnim.BLOCK) {
            return false;
        }
        invBridge.sync();
        return true;
    }

    /**
     * 1.5.2 kept a use's countdown on the player (EntityPlayer.itemInUseCount, vanilla/server EntityPlayer.java:158-161);
     * 1.20.1 keeps it in LivingEntity.useItemRemaining (:219, :3038). FC's UpdateUsingItem gates its warm-up and its
     * every-4-ticks step on it (FCItemCraftingProgressive.java:60-69), so it reads the real counter: Modern-Common's copy
     * (EntityPlayer.java:62) only ever held setItemInUse's 72000.
     */
    // FCMOD-INTEGRATION(1.20.1): held_use: FC's use countdown is the real player's
    @Override
    public int getItemInUseCount() {
        return realPlayer.isUsingItem() ? realPlayer.getUseItemRemainingTicks() : 0;
    }

    /**
     * FC ends a hold-to-craft use with SetItemInUseCount(1) (FCItemCraftingProgressive.java:83; vanilla/server
     * EntityPlayer.java:3222-3225), so the decrement after UpdateUsingItem finishes the use that tick (:229-235). Written
     * to the real counter: ProxyItem.onUseTick runs before updateUsingItem's decrement (LivingEntity.java:2898, :2903).
     */
    // FCMOD-INTEGRATION(1.20.1): held_use: FC's use countdown is the real player's, so a hold-to-craft use can end itself
    @Override
    public void SetItemInUseCount(int count) {
        super.SetItemInUseCount(count);
        if (realPlayer.isUsingItem()) {
            ((btw.forge.mixin.LivingEntityUseAccessor) realPlayer).btw$setUseItemRemaining(count);
        }
    }

    /**
     * FC calls this to play a sound at the player's position.
     * Maps FC sound names to MC SoundEvents via {@link SoundMapping}.
     */
    @Override
    public void playSound(String name, float volume, float pitch) {
        SoundMapping.playAtEntity(realPlayer, name, volume, pitch);
    }

    /**
     * FC calls this when the player can't eat/drink (hunger potion active).
     * Plays a failure sound.
     */
    @Override
    public void OnCantConsume() {
        playSound("random.eat", 0.5F, 0.5F);
    }

    /**
     * FC calls this to open a GUI for a chest/inventory.
     * Delegates to the container bridge which wraps the FC IInventory
     * in an MC 1.20.1 menu and opens it on the real ServerPlayer.
     */
    @Override
    public void displayGUIChest(btw.modern.IInventory inventory) {
        ContainerBridge.openChestGUI(this, inventory);
    }

    /**
     * FC calls this to open the crafting table GUI (work stump, crafting table).
     * Creates an FC ContainerWorkbench and opens it via the FC container bridge,
     * ensuring FC's recipe system (CraftingManager) is used, not vanilla's.
     */
    @Override
    public void displayGUIWorkbench(int x, int y, int z) {
        // Try to create FC's FCContainerWorkbench (extends ContainerWorkbench)
        // to get FC-specific crafting behavior and canInteractWith checks.
        btw.modern.Container fcContainer = null;
        try {
            Class<?> fcClass = Class.forName("net.minecraft.src.btw.crafting.FCContainerWorkbench");
            fcContainer = (btw.modern.Container) fcClass
                    .getConstructor(btw.modern.InventoryPlayer.class, btw.modern.World.class, int.class, int.class, int.class)
                    .newInstance(this.inventory, this.worldObj, x, y, z);
        } catch (Exception e) {
            // Fallback to base ContainerWorkbench
            fcContainer = new btw.modern.ContainerWorkbench(this.inventory, this.worldObj, x, y, z);
        }
        this.openContainer = fcContainer;
        ContainerBridge.openFCContainer(this, fcContainer, "Crafting");
    }

    @Override
    public void displayGUIMerchant(btw.modern.IMerchant merchant, String title) {
        if (merchant == null) return;
        btw.modern.Container fcContainer = new btw.modern.ContainerMerchant(
                this.inventory, merchant, this.worldObj);
        this.openContainer = fcContainer;
        ContainerBridge.openFCContainer(this, fcContainer, title != null ? title : "Merchant");
        // Send recipe list to client so the trade buttons render
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            btw.modern.MerchantRecipeList recipes = merchant.getRecipes(this);
            if (recipes != null) {
                // Get the MC container ID from the opened menu
                int containerId = sp.containerMenu.containerId;
                BTWNetwork.sendMerchantRecipes(sp, containerId, recipes);
            }
        }
    }

    @Override
    public void displayGUIFurnace(btw.modern.TileEntity tileEntity) {
        openTileEntityContainer("btw.modern.ContainerFurnace", tileEntity, "Furnace");
    }

    @Override
    public void displayGUIDispenser(btw.modern.TileEntity tileEntity) {
        openTileEntityContainer("btw.modern.ContainerDispenser", tileEntity, "Dispenser");
    }

    @Override
    public void displayGUIHopper(btw.modern.TileEntity tileEntity) {
        openTileEntityContainer("btw.modern.ContainerHopper", tileEntity, "Hopper");
    }

    @Override
    public void displayGUIBrewingStand(btw.modern.TileEntity tileEntity) {
        openTileEntityContainer("btw.modern.ContainerBrewingStand", tileEntity, "Brewing Stand");
    }

    @Override
    public void displayGUIBeacon(btw.modern.TileEntity tileEntity) {
        openTileEntityContainer("btw.modern.ContainerBeacon", tileEntity, "Beacon");
    }

    /**
     * Creates an FC container via reflection to handle constructor signature
     * mismatches (stubs use IInventory, real classes use specific TileEntity types).
     */
    private void openTileEntityContainer(String containerClassName, btw.modern.TileEntity te, String title) {
        if (te == null) return;
        try {
            Class<?> containerClass = Class.forName(containerClassName);
            // Try (InventoryPlayer, <tileEntityType>) — most real constructors use specific TE types
            for (var ctor : containerClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(this.inventory.getClass())
                        && params[1].isInstance(te)) {
                    btw.modern.Container fc = (btw.modern.Container) ctor.newInstance(this.inventory, te);
                    this.openContainer = fc;
                    ContainerBridge.openFCContainer(this, fc, title);
                    return;
                }
            }
            LOGGER.warn("No matching constructor for {} with {}", containerClassName, te.getClass().getName());
        } catch (Exception e) {
            LOGGER.warn("Failed to open container {}: {}", containerClassName, e.getMessage());
        }
    }

    @Override
    public void displayGUIAnvil(int x, int y, int z) {
        btw.modern.Container fcContainer = new btw.modern.ContainerRepair(
                this.inventory, this.worldObj, x, y, z, this);
        this.openContainer = fcContainer;
        ContainerBridge.openFCContainer(this, fcContainer, "Repairing");
    }

    @Override
    public void displayGUIEnchantment(int x, int y, int z, String name) {
        btw.modern.Container fcContainer = new btw.modern.ContainerEnchantment(
                this.inventory, this.worldObj, x, y, z);
        this.openContainer = fcContainer;
        ContainerBridge.openFCContainer(this, fcContainer, name != null ? name : "Enchant");
    }

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("BTW-PlayerBridge");

    // ================================================================
    // ICrafting bridge — FC's sendProgressBarUpdate → MC DataSlots
    // ================================================================

    /**
     * FC containers call this via the ICrafting interface to sync
     * progress bar data (cook progress, bookshelf level, etc.).
     * Writes the value into the FCContainerMenu's data slot array,
     * which MC automatically syncs to the client via DataSlots.
     */
    @Override
    public void sendProgressBarUpdate(btw.modern.Container container, int id, int value) {
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.containerMenu instanceof FCContainerMenu fcMenu) {
            fcMenu.setFcData(id, value);
        }
    }

    /**
     * FC containers call this to send full slot contents.
     * No-op: MC's broadcastChanges handles slot sync via InventoryAdapter.
     */
    @Override
    public void sendSlotContents(btw.modern.Container container, int slot, btw.modern.ItemStack stack) {
        // No-op — MC handles slot sync
    }

    /**
     * FC containers call this to send all contents at once.
     * No-op: MC's broadcastChanges handles slot sync.
     */
    @Override
    public void sendContainerAndContentsToPlayer(btw.modern.Container container, java.util.List items) {
        // No-op — MC handles slot sync
    }

    @Override
    public void displayGUIEditSign(btw.modern.TileEntity tileEntity) {
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            // Sign editing is handled client-side in 1.20.1 via SignEditPacket
            int x = tileEntity.xCoord;
            int y = tileEntity.yCoord;
            int z = tileEntity.zCoord;
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            net.minecraft.world.level.block.entity.BlockEntity be = sp.level().getBlockEntity(pos);
            if (be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
                sp.openTextEdit(sign, true);
            }
        }
    }
}
