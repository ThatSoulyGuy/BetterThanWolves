package btw.api;

public class RenderSpider extends RenderLiving {

    public RenderSpider() {
        super(new ModelBase() {}, 1.0F);
    }

    protected int setSpiderEyeBrightness(EntitySpider spider, int renderPass, float partialTickTime) {
        return -1;
    }
}
