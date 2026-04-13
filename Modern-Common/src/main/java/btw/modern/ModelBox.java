package btw.modern;

/**
 * Vanilla 1.5.2 ModelBox — creates 6 TexturedQuads with correct UV mapping
 * for the 64x32 entity texture format. Used by ModelRenderer.addBox().
 */
public class ModelBox {
    private PositionTextureVertex[] vertexPositions;
    private TexturedQuad[] quadList;
    public final float posX1, posY1, posZ1;
    public final float posX2, posY2, posZ2;
    public String field_78247_g;

    public ModelBox(ModelRenderer renderer, int texU, int texV,
                    float x, float y, float z, int w, int h, int d, float expand) {
        this.posX1 = x;
        this.posY1 = y;
        this.posZ1 = z;
        this.posX2 = x + (float) w;
        this.posY2 = y + (float) h;
        this.posZ2 = z + (float) d;
        this.vertexPositions = new PositionTextureVertex[8];
        this.quadList = new TexturedQuad[6];

        float x2 = x + (float) w;
        float y2 = y + (float) h;
        float z2 = z + (float) d;
        x -= expand;
        y -= expand;
        z -= expand;
        x2 += expand;
        y2 += expand;
        z2 += expand;

        if (renderer.mirror) {
            float tmp = x2;
            x2 = x;
            x = tmp;
        }

        PositionTextureVertex v0 = new PositionTextureVertex(x, y, z, 0F, 0F);
        PositionTextureVertex v1 = new PositionTextureVertex(x2, y, z, 0F, 8F);
        PositionTextureVertex v2 = new PositionTextureVertex(x2, y2, z, 8F, 8F);
        PositionTextureVertex v3 = new PositionTextureVertex(x, y2, z, 8F, 0F);
        PositionTextureVertex v4 = new PositionTextureVertex(x, y, z2, 0F, 0F);
        PositionTextureVertex v5 = new PositionTextureVertex(x2, y, z2, 0F, 8F);
        PositionTextureVertex v6 = new PositionTextureVertex(x2, y2, z2, 8F, 8F);
        PositionTextureVertex v7 = new PositionTextureVertex(x, y2, z2, 8F, 0F);

        this.vertexPositions[0] = v0;
        this.vertexPositions[1] = v1;
        this.vertexPositions[2] = v2;
        this.vertexPositions[3] = v3;
        this.vertexPositions[4] = v4;
        this.vertexPositions[5] = v5;
        this.vertexPositions[6] = v6;
        this.vertexPositions[7] = v7;

        float tw = renderer.textureWidth;
        float th = renderer.textureHeight;

        this.quadList[0] = new TexturedQuad(new PositionTextureVertex[]{v5, v1, v2, v6},
                texU + d + w, texV + d, texU + d + w + d, texV + d + h, tw, th);
        this.quadList[1] = new TexturedQuad(new PositionTextureVertex[]{v0, v4, v7, v3},
                texU, texV + d, texU + d, texV + d + h, tw, th);
        this.quadList[2] = new TexturedQuad(new PositionTextureVertex[]{v5, v4, v0, v1},
                texU + d, texV, texU + d + w, texV + d, tw, th);
        this.quadList[3] = new TexturedQuad(new PositionTextureVertex[]{v2, v3, v7, v6},
                texU + d + w, texV + d, texU + d + w + w, texV, tw, th);
        this.quadList[4] = new TexturedQuad(new PositionTextureVertex[]{v1, v0, v3, v2},
                texU + d, texV + d, texU + d + w, texV + d + h, tw, th);
        this.quadList[5] = new TexturedQuad(new PositionTextureVertex[]{v4, v5, v6, v7},
                texU + d + w + d, texV + d, texU + d + w + d + w, texV + d + h, tw, th);

        if (renderer.mirror) {
            for (int i = 0; i < this.quadList.length; i++) {
                this.quadList[i].flipFace();
            }
        }
    }

    public void render(Tessellator tessellator, float scale) {
        for (int i = 0; i < this.quadList.length; i++) {
            this.quadList[i].draw(tessellator, scale);
        }
    }

    public ModelBox func_78244_a(String name) {
        this.field_78247_g = name;
        return this;
    }
}
