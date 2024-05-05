package com.hypercube.workshop.synthripper;

public class SynthRipperState {
    int nbChannels;
    float[] loudnessPerChannel;
    float loudness;
    float noiseFloor = -1;
    float maxNoteDurationSec;
    float maxNoteReleaseDurationSec;
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

    public boolean isSilentBuffer() {
        return loudness <= noiseFloor;
    }

    public boolean isFirstVelocity() {
        return velocity == veloIncrement;
    }
}
