package btw.modern;

public class EntityDamageSourceIndirect extends EntityDamageSource {
    private Entity indirectEntity;

    public EntityDamageSourceIndirect(String type, Entity source, Entity indirect) {
        super(type, source);
        this.indirectEntity = indirect;
    }

    public Entity getSourceOfDamage() { return indirectEntity; }
}
