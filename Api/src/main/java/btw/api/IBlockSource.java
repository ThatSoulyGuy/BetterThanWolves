package btw.api;

public interface IBlockSource extends IPosition {
    double getX();
    double getY();
    double getZ();
    int getXInt();
    int getYInt();
    int getZInt();
    int func_82620_h();
    World getWorld();
}
