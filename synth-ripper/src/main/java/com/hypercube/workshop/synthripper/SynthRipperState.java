package com.hypercube.workshop.synthripper;

public class SynthRipperState {
    int nbChannels;
    double[] loudnessPerChannel;
    float loudness;
    float noiseFloor = -1;
    double[] noiseFloorFrequencies;
    double[] signalFrequencies;
    long noiseSamplesRead = 0;

    float maxNoteDurationSec;
    float maxNoteReleaseDurationSec;
    float durationInSec;

    long durationInSamples;
    long noteOffSampleMarker;

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
        //return loudness <= noiseFloor;
        for (int i = 0; i < noiseFloorFrequencies.length; i++) {
            if (signalFrequencies[i] > noiseFloorFrequencies[i] * 2) {
                return false;
            }
        }
        return true;
    }

    public boolean isFirstVelocity() {
        return velocity == veloIncrement;
    }

    public void resetNoiseFloorFrequencies() {
        noiseSamplesRead = 0;
        for (int i = 0; i < noiseFloorFrequencies.length; i++) {
            noiseFloorFrequencies[i] = 0;
        }
    }
}
