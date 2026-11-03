package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Bridges FC gameplay systems that modify vanilla mechanics:
 * <ul>
 *   <li>Mob scroll drops — vanilla (non-FC) mobs roll FC's per-death chance to drop
 *       a specific Arcane Scroll (FC's enchantment carrier items)</li>
 *   <li>Villager scroll trades — max-level villagers sell specific scrolls</li>
 *   <li>FC penalties: BTW's mining penalty on non-FC blocks (Forge BreakSpeed, both sides) and the land-movement
 *       penalty attribute</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public class FCGameplayBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-GameplayBridge");

    /** FC Arcane Scroll itemsList index: ParseID 22223 + 256 offset = 22479. */
    private static final int SCROLL_ITEM_ID = 22479;

    // NO enchanting-table cancel (hook_ownership D5(a)): 4.B kept the 1.5.2 table (vanilla/server
    // BlockEnchantmentTable.java:54-66) and only removed its recipe (FCRecipes.java:5476), using the table as the
    // Infernal Enchanter's ingredient (FCRecipes.java:2518-2526), so a table placed or found opens the 1.20.1
    // EnchantmentMenu. A RightClickBlock cancel here also hid every click on the table from other mods' handlers.

    // ================================================================
    // FC penalty debuffs — mining speed, movement, melee damage
    // ================================================================

    /**
     * BTW's health, hunger, fat and gloom mining penalty on every block FC does not dig itself, on both sides.
     * 1.5.2 multiplied every dig by GetMiningSpeedModifier (inside getCurrentPlayerStrVsBlock); blocks with an FC
     * identity still get it there, through {@link FCMiningSpeed} (ProxyBlocks and FC-mapped vanilla ids, which
     * post this event themselves with FC's strength, penalty included), so they are skipped here and the penalty
     * applies exactly once. Every other block (post-1.5.2 blocks, the bark and stripped log forms, other mods'
     * blocks) digs through Player.getDigSpeed, which posts this event: it gets
     * {@link FCMiningSpeed#miningModifier(Player)}, 1.0 (healthy) down to 0.125, derived the same way on the client
     * (the PenaltySync mirror) and the server, so the client predicts the server's progress (the server accepts a
     * STOP at >= 0.7, ServerPlayerGameMode.java:184-185).
     *
     * <p>This used to read the bridge's m_fCurrentMiningSpeedModifier, which is 1.0 outside FCMiningSpeed's FC call
     * (Modern-Common EntityPlayer.GetMiningSpeedModifier), and only for a ServerPlayer: a no-op everywhere.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_harvest_events: 1.20.1-only blocks dig through Player.getDigSpeed, so BTW's mining penalty reaches them through Forge's BreakSpeed, on both sides
    @SubscribeEvent
    public static void onBreakSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        if (state == null || ProxyRegistry.getFcBlock(state.getBlock()) != null) return; // FCMiningSpeed applied it
        float modifier = FCMiningSpeed.miningModifier(event.getEntity());
        if (modifier < 1.0F) {
            event.setNewSpeed(event.getNewSpeed() * modifier);
        }
    }

    // NO LivingHurtEvent melee penalty. 1.5.2 applies GetMeleeDamageModifier exactly once, to the
    // base melee damage (vanilla/server EntityPlayer.java:1225-1231); the bridge does that in
    // PlayerMixin.btw$applyMeleeDamageModifier. A LivingHurtEvent hook applied it a second time and
    // also hit arrows/thorns/sweep/TNT (any source whose getEntity() is the player). Bow damage is
    // penalised by FC itself via FCItemBow GetBowPullStrengthModifier.

    /**
     * Stable id of the FC land-movement penalty modifier (transient: never saved to NBT).
     * Also read on both sides by FCMovementBonus.getSyncedPenaltyModifier (airborne penalty).
     */
    public static final java.util.UUID FC_PENALTY_UUID =
            java.util.UUID.fromString("b3e3b4a0-fc01-4e3b-8c1a-deadbeef1234");
    private static long btw$lastPenaltyErrorMs = 0L;

    /**
     * FC land-movement penalty (hunger/fat/health/gloom) as a MOVEMENT_SPEED modifier.
     * 1.5.2 recomputes it every tick (vanilla/server EntityLiving.java:1911-1914 +
     * getSpeedModifier:3235 GetLandMovementModifier), so reconcile the modifier with the CURRENT
     * value every tick -- including REMOVING it when the modifier is back to 1.0 (it used to
     * stick forever). Only touch the attribute when the value changes: every add/remove marks it
     * dirty and resends it to the client (ServerEntity.sendDirtyEntityData), which then rebuilds
     * all its MOVEMENT_SPEED modifiers. The attribute syncs to the client by vanilla, so the
     * client-authoritative LocalPlayer.getSpeed() sees the same value.
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        if (!(event.player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        var speedAttr = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        float moveMod = 1.0F;
        try {
            moveMod = PlayerBridge.getOrCreate(sp).GetLandMovementModifier();
        } catch (Throwable t) {
            long now = System.currentTimeMillis();
            if (now - btw$lastPenaltyErrorMs > 10_000L) {
                btw$lastPenaltyErrorMs = now;
                LOGGER.warn("FC GetLandMovementModifier threw: {}: {}", t.getClass().getSimpleName(), t.getMessage());
            }
        }
        // MULTIPLY_TOTAL: value *= (1 + amount)  ->  amount = moveMod - 1 (negative = slower)
        double wanted = moveMod < 0.99F ? (double) moveMod - 1.0D : 0.0D;

        var existing = speedAttr.getModifier(FC_PENALTY_UUID);
        double current = existing != null ? existing.getAmount() : 0.0D;
        if (Math.abs(current - wanted) < 1.0E-6D) return; // unchanged: leave the attribute clean

        if (existing != null) speedAttr.removeModifier(FC_PENALTY_UUID);
        if (wanted != 0.0D) {
            speedAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    FC_PENALTY_UUID, "FC penalty slowdown", wanted,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    // ================================================================
    // Mob Arcane Scroll drops (one roll at death, vanilla mobs only)
    // ================================================================

    private static long btw$lastScrollErrorMs = 0L;

    /**
     * FC's per-death Arcane Scroll roll (EntityLiving.CheckForScrollDrop) for mobs that are still
     * VANILLA 1.20.1 entities. 1.5.2 rolls it exactly once, in EntityLivingOnDeath, server-side,
     * only for non-child mobs with doMobLoot and only when the world is not creative
     * (vanilla/server EntityLiving.java:3343-3369); no living mob ever drops a scroll. This used to
     * run on LivingTickEvent -- FC's per-death odds rolled 20x a second for a mob's whole life
     * (glow squids/drowned seeding cave lakes and rivers with scrolls, triage D2).
     * <p>FC mobs (Proxy* puppets) roll it themselves in their own FC death and are never instances
     * of these vanilla classes, so they never match here; the table is generic so any vanilla mob
     * that is (still) not replaced by an FC mob uses its FC equivalent's odds.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        try {
            if (!(entity.level() instanceof ServerLevel level)) return;
            if (entity.isBaby()) return;
            if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
            // 1.5.2: worldObj.getWorldInfo().getGameType() != CREATIVE -- the WORLD's game type (WorldInfoBridge.isCreativeWorld).
            if (WorldInfoBridge.isCreativeWorld(level.getServer())) return;

            int[] drop = fcScrollDropFor(entity);
            if (drop == null) return;
            int enchantId = drop[0];
            int chance = drop[1];
            if (chance > 1 && entity.getRandom().nextInt(chance) != 0) return;

            net.minecraft.world.item.Item scrollItem = ProxyRegistry.getModernItem(SCROLL_ITEM_ID);
            if (scrollItem == null) return;

            ItemStack scroll = new ItemStack(scrollItem);
            scroll.setDamageValue(enchantId);
            // 1.5.2 entityDropItem(stack, 0F) == Entity.spawnAtLocation(stack, 0F), but added to the
            // event's drop list: captureDrops() is already null here, so spawnAtLocation would bypass
            // the event (and any handler that cancels or edits the drops).
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), scroll);
            item.setDefaultPickUpDelay();
            event.getDrops().add(item);
        } catch (Throwable t) {
            long now = System.currentTimeMillis();
            if (now - btw$lastScrollErrorMs > 10_000L) {
                btw$lastScrollErrorMs = now;
                LOGGER.warn("Arcane scroll drop for {} failed: {}", entity.getType(), t.toString());
            }
        }
    }

    /**
     * {enchantment id (= Arcane Scroll damage value), 1-in-N chance} of the FC mob equivalent to this
     * vanilla mob, from FC's own CheckForScrollDrop overrides; null = FC drops no scroll for it.
     * Order matters: subclasses first (1.20.1 ZombifiedPiglin/Drowned/Husk/ZombieVillager extend
     * Zombie, WitherSkeleton/Stray extend AbstractSkeleton, CaveSpider extends Spider, GlowSquid
     * extends Squid, MagmaCube extends Slime).
     */
    private static int[] fcScrollDropFor(LivingEntity entity) {
        if (entity instanceof ZombifiedPiglin) {
            return new int[] {1, 1000};    // fire protection: Common FCEntityPigZombie.java:66-74
        } else if (entity instanceof Zombie) {
            return new int[] {17, 1000};   // smite: Server FCEntityZombie.java:261-269 (drowned/husk/zombie villager are 1.5.2 zombies)
        } else if (entity instanceof WitherSkeleton) {
            return null;                   // FCEntitySkeleton.java:218 rolls only for skeleton type 0, never wither skeletons
        } else if (entity instanceof AbstractSkeleton) {
            // infinity in the Nether, else projectile protection: Server FCEntitySkeleton.java:216-232
            return new int[] {entity.level().dimension() == Level.NETHER ? 51 : 4, 1000};
        } else if (entity instanceof Spider) {
            return new int[] {18, 1000};   // bane of arthropods: Server FCEntitySpider.java:228-236 (FCEntityCaveSpider inherits)
        } else if (entity instanceof Creeper) {
            return new int[] {3, 1000};    // blast protection: Server FCEntityCreeper.java:194-202
        } else if (entity instanceof EnderMan) {
            return new int[] {33, 1000};   // silk touch: Common FCEntityEnderman.java:308-315
        } else if (entity instanceof Ghast) {
            return new int[] {49, 500};    // punch: Server FCEntityGhast.java:174-182
        } else if (entity instanceof Witch) {
            return new int[] {6, 1000};    // aqua affinity: Common FCEntityWitch.java:74-82
        } else if (entity instanceof Blaze) {
            return new int[] {50, 500};    // flame: Common FCEntityBlaze.java:38-46
        } else if (entity instanceof Bat) {
            return new int[] {2, 250};     // feather falling: Common FCEntityBat.java:29-37
        } else if (entity instanceof Squid) {
            return new int[] {5, 250};     // respiration: Common FCEntitySquid.java:579-586
        } else if (entity instanceof MagmaCube magmaCube) {
            // fire aspect, smallest size only: Common FCEntityMagmaCube.java:37-45
            return magmaCube.getSize() == 1 ? new int[] {20, 250} : null;
        } else if (entity instanceof Slime slime) {
            // protection, smallest size only: Common FCEntitySlime.java:189-197
            return slime.getSize() == 1 ? new int[] {0, 1000} : null;
        } else if (entity instanceof WitherBoss) {
            return new int[] {19, 1};      // knockback, every death: Common FCEntityWither.java:33-38
        }
        return null;
    }

    // ================================================================
    // Villager Arcane Scroll trades
    // ================================================================

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        // FC villagers sell scrolls at max trade level (5).
        // Cost: 1 Paper + N Emeralds → 1 Arcane Scroll
        VillagerProfession prof = event.getType();

        if (prof == VillagerProfession.FARMER) {
            event.getTrades().get(5).add(new ScrollTrade(21, 16, 32)); // Looting
        } else if (prof == VillagerProfession.LIBRARIAN) {
            event.getTrades().get(5).add(new ScrollTrade(48, 32, 48)); // Power
        } else if (prof == VillagerProfession.CLERIC) {
            event.getTrades().get(5).add(new ScrollTrade(35, 48, 64)); // Fortune
        } else if (prof == VillagerProfession.WEAPONSMITH) {
            event.getTrades().get(5).add(new ScrollTrade(34, 32, 48)); // Unbreaking
        } else if (prof == VillagerProfession.BUTCHER) {
            event.getTrades().get(5).add(new ScrollTrade(16, 32, 48)); // Sharpness
        }
    }

    /**
     * Villager trade that sells an Arcane Scroll for Paper + Emeralds.
     */
    private static class ScrollTrade implements VillagerTrades.ItemListing {
        private final int scrollDamage;
        private final int minEmeralds;
        private final int maxEmeralds;

        ScrollTrade(int scrollDamage, int minEmeralds, int maxEmeralds) {
            this.scrollDamage = scrollDamage;
            this.minEmeralds = minEmeralds;
            this.maxEmeralds = maxEmeralds;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            net.minecraft.world.item.Item scrollItem = ProxyRegistry.getModernItem(SCROLL_ITEM_ID);
            if (scrollItem == null) return null;

            ItemStack scroll = new ItemStack(scrollItem);
            scroll.setDamageValue(scrollDamage);

            int emeralds = random.nextIntBetweenInclusive(minEmeralds, maxEmeralds);

            return new MerchantOffer(
                    new ItemStack(Items.PAPER),
                    new ItemStack(Items.EMERALD, emeralds),
                    scroll,
                    3,     // maxUses
                    30,    // xpReward
                    0.05F  // priceMultiplier
            );
        }
    }
}
