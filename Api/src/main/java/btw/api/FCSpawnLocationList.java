package btw.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FCSpawnLocationList {
    public List m_SpawnLocations;

    public FCSpawnLocationList() { m_SpawnLocations = new ArrayList(); }

    public void loadFromNBT(NBTTagList tagList) {
        m_SpawnLocations.clear();
        for (int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound c = (NBTTagCompound)tagList.tagAt(i);
            m_SpawnLocations.add(new FCSpawnLocation(c));
        }
    }

    public NBTTagList saveToNBT() {
        NBTTagList tagList = new NBTTagList("SpawnLocations");
        Iterator it = m_SpawnLocations.iterator();
        while (it.hasNext()) {
            NBTTagCompound c = new NBTTagCompound();
            ((FCSpawnLocation)it.next()).WriteToNBT(c);
            tagList.appendTag(c);
        }
        return tagList;
    }

    public void AddPoint(int iIPos, int iJPos, int iKPos, long lSpawnTime) {
        m_SpawnLocations.add(new FCSpawnLocation(iIPos, iJPos, iKPos, lSpawnTime));
    }

    public FCSpawnLocation GetClosestSpawnLocationForPosition(double dXPos, double dZPos) {
        FCSpawnLocation closest = null;
        double closestDistSq = 0;
        Iterator it = m_SpawnLocations.iterator();
        while (it.hasNext()) {
            FCSpawnLocation p = (FCSpawnLocation)it.next();
            double dI = (double)p.m_iIPos - dXPos;
            double dK = (double)p.m_iKPos - dZPos;
            double distSq = dI * dI + dK * dK;
            if (closest == null || distSq < closestDistSq) { closest = p; closestDistSq = distSq; }
        }
        return closest;
    }

    public FCSpawnLocation GetMostRecentSpawnLocation() {
        FCSpawnLocation mostRecent = null;
        Iterator it = m_SpawnLocations.iterator();
        while (it.hasNext()) {
            FCSpawnLocation p = (FCSpawnLocation)it.next();
            if (mostRecent == null || p.m_lSpawnTime > mostRecent.m_lSpawnTime) mostRecent = p;
        }
        return mostRecent;
    }

    public boolean DoesListContainPoint(int iIPos, int iJPos, int iKPos, long lSpawnTime) {
        Iterator it = m_SpawnLocations.iterator();
        while (it.hasNext()) {
            FCSpawnLocation p = (FCSpawnLocation)it.next();
            if (p.m_iIPos == iIPos && p.m_iKPos == iKPos && p.m_iJPos == iJPos && p.m_lSpawnTime == lSpawnTime)
                return true;
        }
        return false;
    }

    public void AddPointIfNotAlreadyPresent(int iIPos, int iJPos, int iKPos, long lSpawnTime) {
        if (!DoesListContainPoint(iIPos, iJPos, iKPos, lSpawnTime)) AddPoint(iIPos, iJPos, iKPos, lSpawnTime);
    }
}
