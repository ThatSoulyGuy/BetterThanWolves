package btw.modern;

public class StatBase {
    public final int statId;
    public final String statName;
    public boolean isIndependent;

    public StatBase(int id, String name) {
        this.statId = id;
        this.statName = name;
    }

    public StatBase initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public StatBase registerStat() { return this; }
    public String toString() { return statName; }
}
