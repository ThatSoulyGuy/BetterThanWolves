package btw.modern;

public class StepSound {

    public final String stepSoundName;
    public final float stepSoundVolume;
    public final float stepSoundPitch;

    public StepSound(String name, float volume, float pitch) {
        this.stepSoundName = name;
        this.stepSoundVolume = volume;
        this.stepSoundPitch = pitch;
    }

    public float getVolume() {
        return this.stepSoundVolume;
    }

    public float getPitch() {
        return this.stepSoundPitch;
    }

    public String getBreakSound() {
        return "dig." + this.stepSoundName;
    }

    public String getStepSound() {
        return "step." + this.stepSoundName;
    }

    public String getStepResourcePath() {
        return "step." + this.stepSoundName;
    }

    public String getPlaceSound() {
        return getBreakSound();
    }
}
