package btw.modern;
public class RenderSlime extends RenderLiving {
    protected ModelBase renderPassModel2;
    public RenderSlime(ModelBase model, ModelBase model2, float shadow) { super(model, shadow); this.renderPassModel2 = model2; }
    public RenderSlime(ModelBase model, float shadow) { super(model, shadow); }
    public RenderSlime() { super(null, 0); }
}
