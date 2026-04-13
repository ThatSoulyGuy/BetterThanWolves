package btw.modern;

// Compile-time stub — real implementation provided by vanilla remap at runtime
public class EntityJumpHelper {
    private EntityLiving entity;
    private boolean isJumping;

    public EntityJumpHelper(EntityLiving entity) {
        this.entity = entity;
    }

    public void setJumping() {
        this.isJumping = true;
    }

    public void doJump() {
        entity.isJumping = this.isJumping;
        this.isJumping = false;
    }
}
