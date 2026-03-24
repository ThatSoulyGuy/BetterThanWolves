package btw.modern;
public class RenderCreeper extends RenderLiving {
    public RenderCreeper() { super(new ModelCreeper(), 0.5F); }
    public RenderCreeper(ModelBase model, float shadow) { super(model, shadow); }
}
