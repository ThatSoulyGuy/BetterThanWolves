package btw.api;

public class RenderWolf extends RenderLiving {

    public RenderWolf(ModelBase model, ModelBase renderPassModel, float shadowSize) {
        super(model, shadowSize);
        this.setRenderPassModel(renderPassModel);
    }
}
