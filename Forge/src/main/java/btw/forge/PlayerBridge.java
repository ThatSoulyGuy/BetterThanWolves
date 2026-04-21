package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

import java.util.WeakHashMap;

/**
 * Bridges {@link Player} to {@link btw.modern.EntityPlayer}.
 * FC code interacts with players through btw.modern.EntityPlayer fields
 * and methods; this bridge synchronises state in both directions.
 *
 * Works for BOTH ServerPlayer (server-side) and LocalPlayer (client-side)
 * so that FC's mining speed calculations are consistent on both sides.
 *
 * Instances are cached per-Player via {@link #getOrCreate(Player)}.
 */
public class PlayerBridge extends btw.modern.EntityPlayerMP {
    private final Player realPlayer;
    private static final WeakHashMap<Player, PlayerBridge> cache = new WeakHashMap<>();

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
     * Returns (or creates) the PlayerBridge for the given Player.
     * Works for both ServerPlayer and client-side LocalPlayer.
     */
    public static PlayerBridge getOrCreate(Player player) {
        PlayerBridge bridge = cache.computeIfAbsent(player, PlayerBridge::new);
        bridge.syncFromReal();
        return bridge;
    }

    /**
     * Convenience overload for ServerPlayer.
     */
    public static PlayerBridge getOrCreate(ServerPlayer player) {
        return getOrCreate((Player) player);
    }

    private PlayerBridge(Player player) {
        super(null); // btw.modern.EntityPlayer(World) constructor
        this.realPlayer = player;
        this.capBridge = new CapabilitiesBridge(player.getAbilities());
        this.invBridge = new InventoryBridge(player.getInventory());
        this.capabilities = this.capBridge;
        this.inventory = this.invBridge;
        this.inventory.player = this; // InventoryBridge passes null to super(); fix the backref
        this.foodStats = new btw.modern.FoodStats();
        // NetServerHandler bridge — translates FC packets to MC 1.20.1 packets
        this.playerNetServerHandler = new ForgeNetServerHandler(player);
        // World bridge — use ServerLevel if available, otherwise null (client uses it read-only)
        if (player.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
        }
        syncFromReal();
    }

    /**
     * Sync state FROM real MC player TO FC player (call before FC tick).
     */
    public void syncFromReal() {
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
        this.onGround = realPlayer.onGround();
        this.health = (int) realPlayer.getHealth();
        this.fallDistance = realPlayer.fallDistance;
        this.ticksExisted = realPlayer.tickCount;
        this.experienceLevel = realPlayer.experienceLevel;
        this.capBridge.sync();
        this.invBridge.sync();
        if (realPlayer.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
            // Keep difficulty setting current (FC checks it for food/starvation)
            this.worldObj.difficultySetting = sl.getDifficulty().getId();
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
     * Sync state FROM FC player TO real MC player (call after FC tick).
     */
    public void syncToReal() {
        realPlayer.setHealth((float) this.health);
        // Sync velocity if FC modified it (knockback, fling)
        if (this.motionX != realPlayer.getDeltaMovement().x ||
            this.motionY != realPlayer.getDeltaMovement().y ||
            this.motionZ != realPlayer.getDeltaMovement().z) {
            realPlayer.setDeltaMovement(this.motionX, this.motionY, this.motionZ);
        }
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
        this.health = (int) realPlayer.getHealth();
    }

    /**
     * FC calls this for starvation damage, fire damage, etc.
     * Delegates to the real MC player's hurt() method.
     */
    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource source, int amount) {
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp) {
            net.minecraft.world.damagesource.DamageSource mcSource =
                    DamageSourceMapping.getModern(source, sp.serverLevel());
            boolean result = realPlayer.hurt(mcSource, (float) amount);
            this.health = (int) realPlayer.getHealth();
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
            realPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    mobEffect, effect.getDuration(), effect.getAmplifier()));
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
     * Returns whether the player is currently sleeping.
     * FC disables beds, so this should always return false on the server,
     * but we delegate to the real player state for correctness.
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

    /**
     * Sets the FC hardcore spawn chunk for this player.
     * FC code calls this when a player activates a beacon for respawning.
     * The coordinates are stored on the PlayerBridge and persisted via NBT.
     * On respawn, the ServerPlayerMixin hooks on getRespawnPosition/
     * getRespawnDimension read these values to override vanilla spawn.
     */
    @Override
    public void setSpawnChunk(btw.modern.ChunkCoordinates pos, boolean forced) {
        this.m_HardcoreSpawnChunk = pos;
        // Also push to vanilla spawn system so the MC respawn flow picks it up
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp && pos != null) {
            sp.setRespawnPosition(
                    sp.level().dimension(),
                    new net.minecraft.core.BlockPos(pos.posX, pos.posY, pos.posZ),
                    0.0F, forced, false);
        }
    }

    /**
     * Sets the FC hardcore spawn chunk with an explicit dimension.
     * FC tracks dimension separately because beacon spawns can be
     * cross-dimensional (e.g., a Nether beacon sets spawn in the Nether).
     */
    @Override
    public void setSpawnChunk(btw.modern.ChunkCoordinates pos, boolean forced, int dimensionId) {
        this.m_HardcoreSpawnChunk = pos;
        this.m_iSpawnDimension = dimensionId;
        // Also push to vanilla spawn system
        if (realPlayer instanceof net.minecraft.server.level.ServerPlayer sp && pos != null) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = switch (dimensionId) {
                case -1 -> net.minecraft.world.level.Level.NETHER;
                case 1 -> net.minecraft.world.level.Level.END;
                default -> net.minecraft.world.level.Level.OVERWORLD;
            };
            sp.setRespawnPosition(
                    dim,
                    new net.minecraft.core.BlockPos(pos.posX, pos.posY, pos.posZ),
                    0.0F, forced, false);
        }
    }

    /**
     * Returns the FC hardcore spawn chunk, or null if none is set.
     * FC calls this during respawn validation.
     */
    @Override
    public btw.modern.ChunkCoordinates getBedLocation() {
        return this.m_HardcoreSpawnChunk;
    }

    /**
     * Returns true if the FC spawn is set (FC spawns are always "forced"
     * in that they don't depend on a bed being intact).
     */
    @Override
    public boolean isSpawnForced() {
        return this.m_HardcoreSpawnChunk != null;
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
     * Adds exhaustion to the FC food stats system.
     * FC routes all exhaustion through its own FoodStats which uses
     * a 3x resolution (0-60) hunger system.
     */
    @Override
    public void addExhaustion(float amount) {
        this.foodStats.addExhaustion(amount);
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
