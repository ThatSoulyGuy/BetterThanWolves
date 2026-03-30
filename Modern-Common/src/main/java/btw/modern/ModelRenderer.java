package btw.modern;

import java.util.ArrayList;
import java.util.List;

public class ModelRenderer {

    public float textureWidth;
    public float textureHeight;
    public int textureOffsetX;
    public int textureOffsetY;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public boolean mirror;
    public boolean showModel;
    public boolean isHidden;
    public List cubeList;
    public List childModels;
    public final String boxName;
    public float field_82908_p;
    public float field_82907_q;

    /** Stored box definitions for rendering. */
    private final List<float[]> boxes = new ArrayList<>();

    public ModelRenderer(ModelBase model, String name) {
        this.textureWidth = 64.0F;
        this.textureHeight = 32.0F;
        this.mirror = false;
        this.showModel = true;
        this.cubeList = new ArrayList();
        this.boxName = name;
        if (model != null) {
            this.setTextureSize((int) model.textureWidth, (int) model.textureHeight);
        }
    }

    public ModelRenderer(ModelBase model) {
        this(model, null);
    }

    public ModelRenderer(ModelBase model, int texX, int texY) {
        this(model);
        this.setTextureOffset(texX, texY);
    }

    public void addChild(ModelRenderer child) {
        if (this.childModels == null) {
            this.childModels = new ArrayList();
        }
        this.childModels.add(child);
    }

    public ModelRenderer setTextureOffset(int x, int y) {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
        return this;
    }

    public ModelRenderer addBox(String name, float x, float y, float z, int width, int height, int depth) {
        boxes.add(new float[]{x, y, z, width, height, depth, 0});
        return this;
    }

    public ModelRenderer addBox(float x, float y, float z, int width, int height, int depth) {
        boxes.add(new float[]{x, y, z, width, height, depth, 0});
        return this;
    }

    public void addBox(float x, float y, float z, int width, int height, int depth, float scaleFactor) {
        boxes.add(new float[]{x, y, z, width, height, depth, scaleFactor});
    }

    public void setRotationPoint(float x, float y, float z) {
        this.rotationPointX = x;
        this.rotationPointY = y;
        this.rotationPointZ = z;
    }

    /**
     * Renders all boxes through GL11 transforms and Tessellator quads,
     * so the FCEntityRenderer capture pipeline can intercept the geometry.
     */
    public void render(float scale) {
        if (isHidden || !showModel) return;

        GL11.glPushMatrix();

        // Apply rotation point translation
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);

        // Apply rotations (ZYX order, matching MC 1.5.2)
        if (rotateAngleZ != 0) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        if (rotateAngleY != 0) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        if (rotateAngleX != 0) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);

        // Render each box
        for (float[] box : boxes) {
            renderBox(box[0], box[1], box[2], (int) box[3], (int) box[4], (int) box[5], box[6], scale);
        }

        // Render children
        if (childModels != null) {
            for (Object child : childModels) {
                ((ModelRenderer) child).render(scale);
            }
        }

        GL11.glPopMatrix();
    }

    public void renderWithRotation(float scale) {
        render(scale);
    }

    public void postRender(float scale) {
        // Apply transforms without rendering — used for attachment points
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleZ != 0) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        if (rotateAngleY != 0) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        if (rotateAngleX != 0) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
    }

    /**
     * Emits a box as 6 quads through the Tessellator.
     * UV mapping exactly matches MC 1.5.2's ModelBox quad layout.
     */
    private void renderBox(float x, float y, float z, int w, int h, int d, float expand, float scale) {
        float x1 = (x - expand) * scale;
        float y1 = (y - expand) * scale;
        float z1 = (z - expand) * scale;
        float x2 = (x + w + expand) * scale;
        float y2 = (y + h + expand) * scale;
        float z2 = (z + d + expand) * scale;

        if (mirror) {
            float temp = x1; x1 = x2; x2 = temp;
        }

        // UV regions matching MC 1.5.2 ModelBox exactly:
        // u/v = texture offset, d=depth, w=width, h=height, tw/th = texture dimensions
        float u = textureOffsetX, v = textureOffsetY;
        float tw = textureWidth, th = textureHeight;

        Tessellator tess = Tessellator.instance;

        // East face (x2 side) — quadList[0]
        tess.startDrawingQuads();
        tess.setNormal(1, 0, 0);
        tess.addVertexWithUV(x2, y1, z2, (u+d+w)/tw,     (v+d)/th);
        tess.addVertexWithUV(x2, y1, z1, (u+d+w+d)/tw,   (v+d)/th);
        tess.addVertexWithUV(x2, y2, z1, (u+d+w+d)/tw,   (v+d+h)/th);
        tess.addVertexWithUV(x2, y2, z2, (u+d+w)/tw,     (v+d+h)/th);
        tess.draw();

        // West face (x1 side) — quadList[1]
        tess.startDrawingQuads();
        tess.setNormal(-1, 0, 0);
        tess.addVertexWithUV(x1, y1, z1, u/tw,       (v+d)/th);
        tess.addVertexWithUV(x1, y1, z2, (u+d)/tw,   (v+d)/th);
        tess.addVertexWithUV(x1, y2, z2, (u+d)/tw,   (v+d+h)/th);
        tess.addVertexWithUV(x1, y2, z1, u/tw,       (v+d+h)/th);
        tess.draw();

        // Top face (y1 side — model top) — quadList[2]
        tess.startDrawingQuads();
        tess.setNormal(0, -1, 0);
        tess.addVertexWithUV(x2, y1, z2, (u+d+w)/tw,   v/th);
        tess.addVertexWithUV(x1, y1, z2, (u+d)/tw,     v/th);
        tess.addVertexWithUV(x1, y1, z1, (u+d)/tw,     (v+d)/th);
        tess.addVertexWithUV(x2, y1, z1, (u+d+w)/tw,   (v+d)/th);
        tess.draw();

        // Bottom face (y2 side — model bottom) — quadList[3]
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        tess.addVertexWithUV(x2, y2, z1, (u+d+w+w)/tw,   (v+d)/th);
        tess.addVertexWithUV(x1, y2, z1, (u+d+w)/tw,     (v+d)/th);
        tess.addVertexWithUV(x1, y2, z2, (u+d+w)/tw,     v/th);
        tess.addVertexWithUV(x2, y2, z2, (u+d+w+w)/tw,   v/th);
        tess.draw();

        // Front face (z1 side — north) — quadList[4]
        tess.startDrawingQuads();
        tess.setNormal(0, 0, -1);
        tess.addVertexWithUV(x2, y1, z1, (u+d)/tw,     (v+d)/th);
        tess.addVertexWithUV(x1, y1, z1, (u+d+w)/tw,   (v+d)/th);
        tess.addVertexWithUV(x1, y2, z1, (u+d+w)/tw,   (v+d+h)/th);
        tess.addVertexWithUV(x2, y2, z1, (u+d)/tw,     (v+d+h)/th);
        tess.draw();

        // Back face (z2 side — south) — quadList[5]
        tess.startDrawingQuads();
        tess.setNormal(0, 0, 1);
        tess.addVertexWithUV(x1, y1, z2, (u+d+w+d)/tw,     (v+d)/th);
        tess.addVertexWithUV(x2, y1, z2, (u+d+w+d+w)/tw,   (v+d)/th);
        tess.addVertexWithUV(x2, y2, z2, (u+d+w+d+w)/tw,   (v+d+h)/th);
        tess.addVertexWithUV(x1, y2, z2, (u+d+w+d)/tw,     (v+d+h)/th);
        tess.draw();
    }

    public ModelRenderer setTextureSize(int width, int height) {
        this.textureWidth = (float) width;
        this.textureHeight = (float) height;
        return this;
    }

    public void RenderWithScaleToBaseModel(float scale, float scaleX, float scaleY, float scaleZ) {
        if (isHidden || !showModel) return;
        GL11.glPushMatrix();
        GL11.glTranslatef(rotationPointX * scale, rotationPointY * scale, rotationPointZ * scale);
        if (rotateAngleZ != 0) GL11.glRotatef(rotateAngleZ * (180F / (float) Math.PI), 0, 0, 1);
        if (rotateAngleY != 0) GL11.glRotatef(rotateAngleY * (180F / (float) Math.PI), 0, 1, 0);
        if (rotateAngleX != 0) GL11.glRotatef(rotateAngleX * (180F / (float) Math.PI), 1, 0, 0);
        GL11.glScalef(scaleX, scaleY, scaleZ);
        for (float[] box : boxes) {
            renderBox(box[0], box[1], box[2], (int) box[3], (int) box[4], (int) box[5], box[6], scale);
        }
        if (childModels != null) {
            for (Object child : childModels) {
                ((ModelRenderer) child).render(scale);
            }
        }
        GL11.glPopMatrix();
    }
}
