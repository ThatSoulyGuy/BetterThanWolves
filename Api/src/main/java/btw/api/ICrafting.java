package btw.api;

import java.util.List;

public interface ICrafting {
    void sendContainerAndContentsToPlayer(Container container, List items);
    void sendSlotContents(Container container, int slot, ItemStack stack);
    void sendProgressBarUpdate(Container container, int id, int value);
}
