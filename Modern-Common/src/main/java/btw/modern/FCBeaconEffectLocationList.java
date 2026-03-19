package btw.modern;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FCBeaconEffectLocationList {
    public List m_EffectLocations;

    public FCBeaconEffectLocationList() { m_EffectLocations = new ArrayList(); }

    public void loadFromNBT(NBTTagList tagList) {
        m_EffectLocations.clear();
        for (int i = 0; i < tagList.tagCount(); ++i) {
            NBTTagCompound c = (NBTTagCompound)tagList.tagAt(i);
            m_EffectLocations.add(new FCBeaconEffectLocation(c));
        }
    }

    public NBTTagList saveToNBT() {
        NBTTagList tagList = new NBTTagList("EffectLocations");
        Iterator it = m_EffectLocations.iterator();
        while (it.hasNext()) {
            NBTTagCompound c = new NBTTagCompound();
            ((FCBeaconEffectLocation)it.next()).WriteToNBT(c);
            tagList.appendTag(c);
        }
        return tagList;
    }

    public void RemovePointAt(int iIPos, int iJPos, int iKPos) {
        Iterator it = m_EffectLocations.iterator();
        while (it.hasNext()) {
            FCBeaconEffectLocation p = (FCBeaconEffectLocation)it.next();
            if (p.m_iIPos == iIPos && p.m_iKPos == iKPos && p.m_iJPos == iJPos) { it.remove(); return; }
        }
    }

    public void AddPoint(int iIPos, int iJPos, int iKPos, int iEffectLevel, int iRange) {
        m_EffectLocations.add(new FCBeaconEffectLocation(iIPos, iJPos, iKPos, iEffectLevel, iRange));
    }

    public void ChangeEffectLevelOfPointAt(int iIPos, int iJPos, int iKPos, int iPowerLevel, int iRange) {
        FCBeaconEffectLocation p = GetEffectAtLocation(iIPos, iJPos, iKPos);
        if (p != null) { p.m_iEffectLevel = iPowerLevel; p.m_iRange = iRange; }
    }

    public FCBeaconEffectLocation GetEffectAtLocation(int iIPos, int iJPos, int iKPos) {
        Iterator it = m_EffectLocations.iterator();
        while (it.hasNext()) {
            FCBeaconEffectLocation p = (FCBeaconEffectLocation)it.next();
            if (p.m_iIPos == iIPos && p.m_iKPos == iKPos && p.m_iJPos == iJPos) return p;
        }
        return null;
    }

    public int GetMostPowerfulBeaconEffectForLocation(int iIPos, int iKPos) {
        int iMaxLevel = 0;
        Iterator it = m_EffectLocations.iterator();
        while (it.hasNext()) {
            FCBeaconEffectLocation p = (FCBeaconEffectLocation)it.next();
            if (iIPos >= p.m_iIPos - p.m_iRange && iIPos <= p.m_iIPos + p.m_iRange &&
                iKPos >= p.m_iKPos - p.m_iRange && iKPos <= p.m_iKPos + p.m_iRange) {
                if (p.m_iEffectLevel > iMaxLevel) iMaxLevel = p.m_iEffectLevel;
            }
        }
        return iMaxLevel;
    }
}
