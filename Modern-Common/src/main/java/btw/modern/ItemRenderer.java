package btw.modern;

/**
 * 1.5.2 ItemRenderer — only the static 2D-item extruded-quad emitter is
 * bridged (vanilla/client ItemRenderer.java renderItemIn2D, verbatim): it is
 * pure Tessellator geometry, consumed by RenderItem.renderDroppedItem for the
 * items FC tile renderers embed (placed tools, campfire food, basket
 * contents). The first-person hand-rendering paths belong to the modern
 * engine and are intentionally not bridged.
 */
public class ItemRenderer {

    public static void renderItemIn2D(Tessellator tess, float maxU, float minV, float minU, float maxV, int sheetWidth, int sheetHeight, float thickness) {
        tess.startDrawingQuads();
        tess.setNormal(0.0F, 0.0F, 1.0F);
        tess.addVertexWithUV(0.0D, 0.0D, 0.0D, (double) maxU, (double) maxV);
        tess.addVertexWithUV(1.0D, 0.0D, 0.0D, (double) minU, (double) maxV);
        tess.addVertexWithUV(1.0D, 1.0D, 0.0D, (double) minU, (double) minV);
        tess.addVertexWithUV(0.0D, 1.0D, 0.0D, (double) maxU, (double) minV);
        tess.draw();
        tess.startDrawingQuads();
        tess.setNormal(0.0F, 0.0F, -1.0F);
        tess.addVertexWithUV(0.0D, 1.0D, (double) (0.0F - thickness), (double) maxU, (double) minV);
        tess.addVertexWithUV(1.0D, 1.0D, (double) (0.0F - thickness), (double) minU, (double) minV);
        tess.addVertexWithUV(1.0D, 0.0D, (double) (0.0F - thickness), (double) minU, (double) maxV);
        tess.addVertexWithUV(0.0D, 0.0D, (double) (0.0F - thickness), (double) maxU, (double) maxV);
        tess.draw();
        float uSpan = (float) sheetWidth * (maxU - minU);
        float vSpan = (float) sheetHeight * (maxV - minV);
        tess.startDrawingQuads();
        tess.setNormal(-1.0F, 0.0F, 0.0F);
        int i;
        float slice;
        float u;

        for (i = 0; (float) i < uSpan; ++i) {
            slice = (float) i / uSpan;
            u = maxU + (minU - maxU) * slice - 0.5F / (float) sheetWidth;
            tess.addVertexWithUV((double) slice, 0.0D, (double) (0.0F - thickness), (double) u, (double) maxV);
            tess.addVertexWithUV((double) slice, 0.0D, 0.0D, (double) u, (double) maxV);
            tess.addVertexWithUV((double) slice, 1.0D, 0.0D, (double) u, (double) minV);
            tess.addVertexWithUV((double) slice, 1.0D, (double) (0.0F - thickness), (double) u, (double) minV);
        }

        tess.draw();
        tess.startDrawingQuads();
        tess.setNormal(1.0F, 0.0F, 0.0F);
        float sliceEnd;

        for (i = 0; (float) i < uSpan; ++i) {
            slice = (float) i / uSpan;
            u = maxU + (minU - maxU) * slice - 0.5F / (float) sheetWidth;
            sliceEnd = slice + 1.0F / uSpan;
            tess.addVertexWithUV((double) sliceEnd, 1.0D, (double) (0.0F - thickness), (double) u, (double) minV);
            tess.addVertexWithUV((double) sliceEnd, 1.0D, 0.0D, (double) u, (double) minV);
            tess.addVertexWithUV((double) sliceEnd, 0.0D, 0.0D, (double) u, (double) maxV);
            tess.addVertexWithUV((double) sliceEnd, 0.0D, (double) (0.0F - thickness), (double) u, (double) maxV);
        }

        tess.draw();
        tess.startDrawingQuads();
        tess.setNormal(0.0F, 1.0F, 0.0F);

        for (i = 0; (float) i < vSpan; ++i) {
            slice = (float) i / vSpan;
            u = maxV + (minV - maxV) * slice - 0.5F / (float) sheetHeight;
            sliceEnd = slice + 1.0F / vSpan;
            tess.addVertexWithUV(0.0D, (double) sliceEnd, 0.0D, (double) maxU, (double) u);
            tess.addVertexWithUV(1.0D, (double) sliceEnd, 0.0D, (double) minU, (double) u);
            tess.addVertexWithUV(1.0D, (double) sliceEnd, (double) (0.0F - thickness), (double) minU, (double) u);
            tess.addVertexWithUV(0.0D, (double) sliceEnd, (double) (0.0F - thickness), (double) maxU, (double) u);
        }

        tess.draw();
        tess.startDrawingQuads();
        tess.setNormal(0.0F, -1.0F, 0.0F);

        for (i = 0; (float) i < vSpan; ++i) {
            slice = (float) i / vSpan;
            u = maxV + (minV - maxV) * slice - 0.5F / (float) sheetHeight;
            tess.addVertexWithUV(1.0D, (double) slice, 0.0D, (double) minU, (double) u);
            tess.addVertexWithUV(0.0D, (double) slice, 0.0D, (double) maxU, (double) u);
            tess.addVertexWithUV(0.0D, (double) slice, (double) (0.0F - thickness), (double) maxU, (double) u);
            tess.addVertexWithUV(1.0D, (double) slice, (double) (0.0F - thickness), (double) minU, (double) u);
        }

        tess.draw();
    }
}
