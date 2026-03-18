package btw.api;

public class RenderPig extends RenderLiving {

    public RenderPig(ModelBase model, ModelBase renderPassModel, float shadowSize) {
        super(model, shadowSize);
        this.setRenderPassModel(renderPassModel);
    }
}
