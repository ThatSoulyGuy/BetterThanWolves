package btw.modern;
public class RenderSkeleton extends RenderBiped {
    public RenderSkeleton() { super(new ModelSkeleton(), 0.5F); }
    public RenderSkeleton(ModelBase model, float shadow) { super(model, shadow); }
}
