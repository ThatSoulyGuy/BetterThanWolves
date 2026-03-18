package btw.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class ModelBase {

    public float onGround;
    public boolean isRiding = false;
    public List boxList = new ArrayList();
    public boolean isChild = true;
    public int textureWidth = 64;
    public int textureHeight = 32;

    /**
     * Sets the models various rotation angles then renders the model.
     */
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {}

    /**
     * Sets the model's various rotation angles.
     */
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {}

    /**
     * Used for easily adding entity-dependent animations.
     */
    public void setLivingAnimations(EntityLiving entityLiving, float f, float f1, float f2) {}

    public ModelRenderer getRandomModelBox(Random random) {
        return (ModelRenderer) this.boxList.get(random.nextInt(this.boxList.size()));
    }

    protected void setTextureOffset(String name, int x, int y) {}

    public Object getTextureOffset(String name) {
        return null;
    }
}
