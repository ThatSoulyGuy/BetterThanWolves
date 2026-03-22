package btw.forge;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Bridges FC gameplay systems that modify vanilla mechanics:
 * <ul>
 *   <li>Disables the vanilla enchanting table (FC uses the Infernal Enchanter)</li>
 *   <li>Mob scroll drops — each mob type has a per-tick chance to drop
 *       a specific Arcane Scroll (FC's enchantment carrier items)</li>
 *   <li>Villager scroll trades — max-level villagers sell specific scrolls</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public class FCGameplayBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-GameplayBridge");

    /** FC Arcane Scroll itemsList index: ParseID 22223 + 256 offset = 22479. */
    private static final int SCROLL_ITEM_ID = 22479;

    // ================================================================
    // Disable vanilla enchanting table
    // ================================================================

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock()
                instanceof EnchantmentTableBlock) {
            event.setCanceled(true);
        }
    }

    // ================================================================
    // FC penalty debuffs — mining speed, movement, melee damage
    // ================================================================

    /**
     * Applies FC's mining speed penalty modifier.
     * FC's GetMiningSpeedModifier() ranges from 0.25 (near death) to 1.0 (healthy).
     */
    @SubscribeEvent
    public static void onBreakSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            float modifier = pb.GetMiningSpeedModifier();
            if (modifier < 1.0F) {
                event.setNewSpeed(event.getNewSpeed() * modifier);
            }
        }
    }

    /**
     * Applies FC's melee damage penalty modifier.
     * Multiplies attack damage by GetMeleeDamageModifier() (0.25-1.0).
     */
    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            float modifier = pb.GetMeleeDamageModifier();
            if (modifier < 1.0F) {
                event.setAmount(event.getAmount() * modifier);
            }
        }
    }

    /**
     * Applies FC's movement speed penalty modifier.
     * Reduces walk/sprint speed based on hunger/health/fat/gloom penalties.
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        if (!(event.player instanceof net.minecraft.server.level.ServerPlayer sp)) return;

        PlayerBridge pb = PlayerBridge.getOrCreate(sp);
        float moveMod = pb.GetHealthAndExhaustionModifierWithSightlessModifier();

        if (moveMod < 1.0F) {
            // Apply movement slowdown via a transient attribute modifier
            var speedAttr = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                java.util.UUID FC_PENALTY_UUID = java.util.UUID.fromString("b3e3b4a0-fc01-4e3b-8c1a-deadbeef1234");
                speedAttr.removeModifier(FC_PENALTY_UUID);
                if (moveMod < 0.99F) {
                    speedAttr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            FC_PENALTY_UUID, "FC penalty slowdown",
                            moveMod - 1.0, // negative = slower
                            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
        }
    }

    // ================================================================
    // Mob Arcane Scroll drops (per-tick chance while alive)
    // ================================================================

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int scrollDamage = -1;
        int chance = 1000;

        // Each mob type drops a specific scroll enchantment.
        // Damage value = MC 1.5.2 enchantment ID on the Arcane Scroll.
        if (entity instanceof Zombie && !(entity instanceof ZombifiedPiglin)) {
            scrollDamage = 17; // Smite
        } else if (entity instanceof WitherSkeleton) {
            scrollDamage = 51; // Infinity
        } else if (entity instanceof Skeleton) {
            scrollDamage = 4;  // Projectile Protection
        } else if (entity instanceof Spider) {
            scrollDamage = 18; // Bane of Arthropods
        } else if (entity instanceof Creeper) {
            scrollDamage = 3;  // Blast Protection
        } else if (entity instanceof EnderMan) {
            scrollDamage = 33; // Silk Touch
        } else if (entity instanceof Ghast) {
            scrollDamage = 6;  // Aqua Affinity
            chance = 500;
        } else if (entity instanceof Witch) {
            scrollDamage = 34; // Unbreaking
        } else if (entity instanceof Blaze) {
            scrollDamage = 20; // Fire Aspect
            chance = 500;
        } else if (entity instanceof Bat) {
            scrollDamage = 2;  // Feather Falling
            chance = 250;
        } else if (entity instanceof Squid) {
            scrollDamage = 5;  // Respiration
            chance = 250;
        } else if (entity instanceof MagmaCube mc && mc.getSize() == 1) {
            scrollDamage = 1;  // Fire Protection
            chance = 250;
        } else if (entity instanceof Slime slime
                && !(entity instanceof MagmaCube)
                && slime.getSize() == 1) {
            scrollDamage = 0;  // Protection
        } else if (entity instanceof ZombifiedPiglin) {
            scrollDamage = 1;  // Fire Protection
        } else if (entity instanceof WitherBoss) {
            scrollDamage = 19; // Knockback
            chance = 100;      // Frequent but not every tick
        }

        if (scrollDamage < 0) return;
        if (entity.getRandom().nextInt(chance) != 0) return;

        net.minecraft.world.item.Item scrollItem = ProxyRegistry.getModernItem(SCROLL_ITEM_ID);
        if (scrollItem == null) return;

        ItemStack scroll = new ItemStack(scrollItem);
        scroll.setDamageValue(scrollDamage);
        entity.spawnAtLocation(scroll, 0.0F);
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
