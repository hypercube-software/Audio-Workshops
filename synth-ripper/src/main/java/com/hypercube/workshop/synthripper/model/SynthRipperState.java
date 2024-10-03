package com.hypercube.workshop.synthripper.model;


import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;

public class SynthRipperState {
    public int nbChannels;
    public double[] loudnessPerChannel;
    public float loudness;
    public float noiseFloor = -1;
    public double[] noiseFloorFrequencies;
    public double[] signalFrequencies;
    public long noiseSamplesRead = 0;

    public float maxNoteDurationSec;
    public float maxNoteReleaseDurationSec;
    public float durationInSec;

    public long durationInSamples;
    public long noteOffSampleMarker;

    public int velocity;
    public int note;
    public MidiPreset preset;
    public int presetIndex;
    public int lowestNote;
    public int highestNote;
    public int noteIncrement;
    public int veloIncrement;
    public int upperBoundVelocity;

    public SynthRipperStateEnum state = SynthRipperStateEnum.GET_NOISE_FLOOR;

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
