package btw.modern;
public class RenderZombie extends RenderBiped {
    public RenderZombie() { super(new ModelZombie(), 0.5F); }
    public RenderZombie(ModelBase model, float shadow) { super(model, shadow); }
}
