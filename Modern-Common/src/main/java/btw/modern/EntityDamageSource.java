package btw.modern;

public class EntityDamageSource extends DamageSource {
    private Entity damageSourceEntity;

    public EntityDamageSource(String type, Entity entity) {
        super(type);
        this.damageSourceEntity = entity;
    }

    public Entity getEntity() { return damageSourceEntity; }
    public Entity getSourceOfDamage() { return damageSourceEntity; }
}
