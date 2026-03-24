package btw.modern;
public class RenderBiped extends RenderLiving {
    public RenderBiped(ModelBase model, float shadow) { super(model, shadow); }
    public RenderBiped() { super(new ModelBiped(), 0.5F); }
}
