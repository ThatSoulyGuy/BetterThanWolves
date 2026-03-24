package btw.modern;
public class RenderGhast extends RenderLiving {
    public RenderGhast() { super(new ModelGhast(), 0.5F); }
    public RenderGhast(ModelBase model, float shadow) { super(model, shadow); }
}
