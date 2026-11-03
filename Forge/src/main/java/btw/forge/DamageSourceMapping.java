package btw.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps FC legacy DamageSource constants to MC 1.20.1 DamageSource instances.
 * FC code creates damage via static fields like DamageSource.starve,
 * DamageSource.onFire, etc. This mapper translates them.
 */
public class DamageSourceMapping {

    private static final Logger LOGGER = LogManager.getLogger("BTW-DamageSourceMapping");

    /** Entity classes whose FC-damage hurt() already failed once (logged once per class). */
    private static final Set<String> HURT_FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    /** Review M2: FC's armour-bypassing mob hit (data/betterthanwolves/damage_type/mob_attack_unblockable.json). */
    static final ResourceKey<DamageType> MOB_ATTACK_UNBLOCKABLE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation(BTWForgeMod.MOD_ID, "mob_attack_unblockable"));

    private static volatile boolean unblockableMissingLogged;

    /**
     * Maps an FC DamageSource to the modern MC equivalent.
     *
     * @param fcSource the FC DamageSource (btw.modern.DamageSource)
     * @param level    the server level (needed for 1.20.1 DamageSource registry)
     * @return the modern MC DamageSource, or generic if unmapped
     */
    public static DamageSource getModern(btw.modern.DamageSource fcSource, ServerLevel level) {
        if (fcSource == null) return level.damageSources().generic();

        DamageSources sources = level.damageSources();
        String type = fcSource.damageType;

        if (type == null) return sources.generic();

        // For entity-sourced damage, resolve the MC proxy entity so MC's
        // knockback, death messages, and combat tracker work correctly.
        net.minecraft.world.entity.Entity mcAttacker = null;
        if (fcSource instanceof btw.modern.EntityDamageSource eds) {
            btw.modern.Entity fcAttacker = eds.getEntity();
            if (fcAttacker != null) {
                mcAttacker = resolveProxy(fcAttacker, level);
            }
        }
        // 1.5.2 DamageSource.causeArrowDamage / causeThrownDamage / causeFireballDamage / causeIndirectMagicDamage
        // build EntityDamageSourceIndirect(type, projectile, shooter) (vanilla/server DamageSource.java:70-91):
        // getSourceOfDamage() is the projectile (the MC direct entity), getEntity() the shooter (the causing one).
        net.minecraft.world.entity.Entity mcDirect = mcAttacker;
        boolean indirect = fcSource instanceof btw.modern.EntityDamageSourceIndirect;
        if (indirect) {
            btw.modern.Entity fcDirect = ((btw.modern.EntityDamageSourceIndirect) fcSource).getSourceOfDamage();
            mcDirect = fcDirect != null ? resolveProxy(fcDirect, level) : null;
        }

        return switch (type) {
            // FCMOD-INTEGRATION(1.20.1): head_crab: a mob's hit FC flagged setDamageBypassesArmor (the squid head crab's bite, Common
            // FCEntitySquid.java:944-948) skips armour, shields and the hunger cost, as 1.5.2's unblockable hits did
            case "mob" -> {
                net.minecraft.world.entity.LivingEntity le =
                        mcAttacker instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
                yield fcSource.isUnblockable() ? unblockableMobAttack(level, le) : sources.mobAttack(le);
            }
            case "player" -> sources.playerAttack(mcAttacker instanceof net.minecraft.world.entity.player.Player p
                    ? p : null);
            // Projectiles: DamageSources.arrow / fireball take an AbstractArrow / Fireball, which an FC projectile's
            // MC entity (a ProxyEntity) is not, so these are built from the registry holder as DamageSources does.
            // Same mapping as WorldBridge.toMcDamageSource (which resolves bridges as well as proxies).
            case "arrow" -> indirectSource(level, DamageTypes.ARROW, mcDirect, mcAttacker);
            case "thrown" -> indirectSource(level, DamageTypes.THROWN, mcDirect, mcAttacker);
            // DamageSources.fireball: unattributed when there is no shooter
            case "fireball" -> indirectSource(level,
                    mcAttacker != null ? DamageTypes.FIREBALL : DamageTypes.UNATTRIBUTED_FIREBALL, mcDirect, mcAttacker);
            case "indirectMagic" -> indirectSource(level, DamageTypes.INDIRECT_MAGIC, mcDirect, mcAttacker);
            case "starve" -> sources.starve();
            // causeFireballDamage without a shooter is EntityDamageSourceIndirect("onFire", fireball, fireball)
            case "onFire" -> indirect
                    ? indirectSource(level, DamageTypes.UNATTRIBUTED_FIREBALL, mcDirect, mcAttacker)
                    : sources.onFire();
            case "inFire" -> sources.inFire();
            case "lava" -> sources.lava();
            case "inWall" -> sources.inWall();
            case "drown" -> sources.drown();
            case "fall" -> sources.fall();
            case "outOfWorld" -> sources.fellOutOfWorld();
            case "generic" -> sources.generic();
            case "magic" -> sources.magic();
            case "wither" -> sources.wither();
            // 1.5.2 DamageSource.setExplosionSource (vanilla/server DamageSource.java:101-104): "explosion", or
            // EntityDamageSource "explosion.player" naming Explosion.func_94613_c (the igniter / living exploder);
            // both setDifficultyScaled().setExplosion(). FC mining charges (FCExplosionMining.DamageEntities) pass
            // null, i.e. "explosion". DamageSources.explosion gives EXPLOSION, or PLAYER_EXPLOSION with an entity:
            // is_explosion tag (Blast Protection, explosion death message) and scaling ALWAYS, as setDifficultyScaled.
            case "explosion" -> sources.explosion((net.minecraft.world.entity.Entity) null, null);
            // 1.5.2 EntityDamageSource.isDifficultyScaled overrides the flag (vanilla/server
            // EntityDamageSource.java:45-48): never scaled when the named entity is a player.
            case "explosion.player" -> mcAttacker instanceof net.minecraft.world.entity.player.Player
                    ? withoutDifficultyScaling(sources.explosion(mcAttacker, mcAttacker))
                    : sources.explosion(mcAttacker, mcAttacker);
            case "anvil" -> sources.anvil(null);
            case "fallingBlock" -> sources.fallingBlock(null);
            case "cactus" -> sources.cactus();
            case "lightningBolt" -> sources.lightningBolt();
            // fcGloom is constructed with setDamageBypassesArmor() FC-side; map to magic
            // (in the bypasses_armor damage-type tag) so gloom bites ignore armor.
            case "fcGloom" -> sources.magic();
            default -> sources.generic();
        };
    }

    /** A source of {@code type} with separate direct and causing entities, as DamageSources.source(key, direct, causing). */
    private static DamageSource indirectSource(ServerLevel level, ResourceKey<DamageType> type,
                                               net.minecraft.world.entity.Entity direct,
                                               net.minecraft.world.entity.Entity causing) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), direct, causing);
    }

    /** {@link #MOB_ATTACK_UNBLOCKABLE} from {@code attacker}; mob_attack (logged once) if the type did not load. */
    private static DamageSource unblockableMobAttack(ServerLevel level, net.minecraft.world.entity.LivingEntity attacker) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(MOB_ATTACK_UNBLOCKABLE)
                .<DamageSource>map(h -> new DamageSource(h, attacker))
                .orElseGet(() -> {
                    if (!unblockableMissingLogged) {
                        unblockableMissingLogged = true;
                        LOGGER.warn("Damage type {} is missing; FC's armour-bypassing mob hits use mob_attack (logged once)",
                                MOB_ATTACK_UNBLOCKABLE.location());
                    }
                    return level.damageSources().mobAttack(attacker);
                });
    }

    /** BridgeSelfAudit.runServerStart: the BTW damage type loaded and bypasses armour. */
    static void audit() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var holder = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(MOB_ATTACK_UNBLOCKABLE);
        if (holder.isPresent() && holder.get().is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) {
            LOGGER.info("[SELF-AUDIT/damage] {} loaded, in minecraft:bypasses_armor", MOB_ATTACK_UNBLOCKABLE.location());
        } else {
            LOGGER.warn("[SELF-AUDIT/damage] {} {}: the squid head crab's bite is cut by armour", MOB_ATTACK_UNBLOCKABLE.location(),
                    holder.isPresent() ? "is not in minecraft:bypasses_armor" : "did not load");
        }
    }

    /**
     * {@code source} with 1.20.1's difficulty scaling of player damage switched off; same type, entities and
     * position, so attribution, shields, protection enchantments and the death message are unchanged. For FC
     * indirect (projectile) damage on a player: 1.5.2 EntityPlayer.attackEntityFrom scaled only a source with
     * isDifficultyScaled() (vanilla/server EntityPlayer.java:942-958), which EntityDamageSource answers from the
     * DIRECT entity being a non-player EntityLiving (EntityDamageSource.java:45-48) -- never true for an
     * EntityDamageSourceIndirect, whose direct entity is the arrow / thrown item / fireball. 1.20.1
     * DamageSource.scalesWithDifficulty tests the CAUSING entity, so Player.hurt would scale an FC skeleton's or
     * ghast's projectile (x1.5 on Hard, x/2+1 on Easy, 0 on Peaceful).
     */
    public static DamageSource withoutDifficultyScaling(DamageSource source) {
        if (source == null || !source.scalesWithDifficulty()) return source;
        return new DamageSource(source.typeHolder(), source.getDirectEntity(), source.getEntity(),
                source.sourcePositionRaw()) {
            @Override
            public boolean scalesWithDifficulty() {
                return false;
            }
        };
    }

    /**
     * FC damage on a vanilla MC entity behind a generic bridge (LivingEntityBridge, EntityBridge): MC's own hurt()
     * with the FC source mapped by {@link WorldBridge#toMcDamageSource}, returning hurt()'s result. As 1.5.2
     * attackEntityFrom does, a rejected hit (invulnerability window, an enderman dodging a projectile, an
     * invulnerable entity) answers false, which the frozen projectiles treat as a ricochet (vanilla/server
     * EntityArrow.java:308-347). Server only (false on the client, where MC hurt() does nothing). Never throws: a
     * failure reads as a rejected hit and is logged once per entity class.
     */
    static boolean hurtFromFc(net.minecraft.world.entity.Entity target, btw.modern.DamageSource fcSource, int amount) {
        if (target == null || !(target.level() instanceof ServerLevel sl)) return false;
        try {
            return target.hurt(WorldBridge.toMcDamageSource(fcSource, sl), (float) amount);
        } catch (Throwable t) {
            if (HURT_FAILURES_LOGGED.add(target.getClass().getName())) {
                LOGGER.warn("FC damage '{}' on {} failed; treated as a rejected hit (logged once per entity class): {}",
                        fcSource != null ? fcSource.damageType : null, target.getClass().getSimpleName(), t.toString(), t);
            }
            return false;
        }
    }

    /**
     * Finds the MC proxy entity for an FC entity.
     */
    private static net.minecraft.world.entity.Entity resolveProxy(btw.modern.Entity fcEntity, ServerLevel level) {
        // Search by entity ID — the proxy and FC entity share the same ID.
        return level.getEntity(fcEntity.entityId);
    }
}
