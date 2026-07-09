package btw.modern;

public class EntityDamageSource extends DamageSource {
    // 1.5.2 EntityDamageSource — field is protected so EntityDamageSourceIndirect
    // can return the DIRECT damager (projectile) from getSourceOfDamage().
    protected Entity damageSourceEntity;

    public EntityDamageSource(String type, Entity entity) {
        super(type);
        this.damageSourceEntity = entity;
    }

    public Entity getEntity() { return damageSourceEntity; }
    public Entity getSourceOfDamage() { return damageSourceEntity; }
}
