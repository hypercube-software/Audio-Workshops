package com.hypercube.workshop.synthripper;

public class SynthRipperState {
    public int nbChannels;
    public float[] loudnessPerChannel;
    public float loudness;
    public float noiseFloor;
    public float maxNoteDurationSec;
    public float maxNoteReleaseDurationSec;
    float durationInSec;
    int velocity;
    int note;
    int cc;
    int lowestCC;
    int highestCC;
    int lowestNote;
    int highestNote;
    int noteIncrement;
    int veloIncrement;
    int upperBoundVelocity;
    SynthRipperStateEnum state = SynthRipperStateEnum.GET_NOISE_FLOOR;

    public int getLoudnessDb() {
        return (int) (20 * Math.log10(loudness));
    }

    public int getNoiseFloorDb() {
        return (int) (20 * Math.log10(noiseFloor));
    }

    public boolean isFirstVelocity() {
        return velocity == veloIncrement;
    }
}
