package btw.modern;

public class RenderFallingSand extends Render {

    public RenderFallingSand() {
        this.shadowSize = 0.5F;
    }

    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {}

    public void doRenderFallingSand(EntityFallingSand entity, double x, double y, double z, float yaw, float partialTickTime) {}
}
