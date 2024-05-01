package com.hypercube.workshop.synthripper;

public class SynthRipperState {
    public int nbChannels;
    public float[] loudnessPerChannel;
    public float loudness;
    public float noiseFloor;
    public float maxNoteDurationSec;
    public float maxNoteReleaseDurationSec;
    float durationInSec;
    int note;
    int cc;
    int lowestCC;
    int highestCC;
    int lowestNote;
    int highestNote;
    int noteIncrement;
    SynthRipperStateEnum state = SynthRipperStateEnum.GET_NOISE_FLOOR;

    public int getLoudnessDb() {
        return (int) (20 * Math.log10(loudness));
    }

    public int getNoiseFloorDb() {
        return (int) (20 * Math.log10(noiseFloor));
    }
}
