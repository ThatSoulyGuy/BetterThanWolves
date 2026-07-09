package btw.modern;

public class EntityDamageSourceIndirect extends EntityDamageSource {
    private Entity indirectEntity;

    public EntityDamageSourceIndirect(String type, Entity source, Entity indirect) {
        super(type, source);
        this.indirectEntity = indirect;
    }

    // 1.5.2 EntityDamageSourceIndirect.getSourceOfDamage — returns the DIRECT
    // damager (the arrow/fireball entity itself).
    public Entity getSourceOfDamage() { return this.damageSourceEntity; }

    // 1.5.2 EntityDamageSourceIndirect.getEntity — returns the TRUE source
    // (the shooter). Frozen EntityLiving.attackEntityFrom/onDeath call
    // getEntity() for revenge-targeting, recentlyHit player kill credit,
    // and wolf/pigman pack aggro; the previous shim had the two methods
    // swapped, so ranged kills credited the projectile instead.
    public Entity getEntity() { return this.indirectEntity; }
}
