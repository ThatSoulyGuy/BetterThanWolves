package btw.api;

public interface Icon {
    int getOriginX();
    int getOriginY();
    float getMinU();
    float getMaxU();
    float getInterpolatedU(double d);
    float getMinV();
    float getMaxV();
    float getInterpolatedV(double d);
    String getIconName();
    int getSheetWidth();
    int getSheetHeight();
    int getIconWidth();
    int getIconHeight();
}
