package com.hypercube.workshop.synthripper.model;


import java.util.ArrayList;
import java.util.List;

import static com.hypercube.workshop.synthripper.SynthRipper.NOISE_FLOOR_CAPTURE_DURATION_IN_SEC;

public class SynthRipperState {
    public int nbChannels;
    public double[] noiseFloorFrequencies;
    public double[] signalFrequencies;
    public long noiseSamplesRead = 0;

    public float maxNoteDurationSec;
    public float maxNoteReleaseDurationSec;
    public float durationInSec;

    public long durationInSamples;
    public long noteOffSampleMarker;

    public SynthRipperStateEnum state = SynthRipperStateEnum.INIT;

    public RecordedSynthNote prev;
    public int currentBatchEntry = 0;
    public List<RecordedSynthNote> sampleBatch = new ArrayList<>();

    public boolean isSilentBuffer() {
        for (int i = 0; i < noiseFloorFrequencies.length; i++) {
            if (signalFrequencies[i] > noiseFloorFrequencies[i] * 2) {
                return false;
            }
        }
        return true;
    }

    public RecordedSynthNote getCurrentRecordedSynthNote() {
        return currentBatchEntry < sampleBatch.size() ? sampleBatch.get(currentBatchEntry) : null;
    }

    public void nextRecordedSynthNote() {
        if (currentBatchEntry < sampleBatch.size()) {
            currentBatchEntry++;
        }
    }

    public boolean isFirstVelocity() {
        return getCurrentRecordedSynthNote().isFirstVelocity();
    }

    public void resetNoiseFloorFrequencies() {
        noiseSamplesRead = 0;
        for (int i = 0; i < noiseFloorFrequencies.length; i++) {
            noiseFloorFrequencies[i] = 0;
        }
    }

    public void changeState(SynthRipperStateEnum newState) {
        state = newState;
        durationInSec = 0;
        durationInSamples = 0;
    }

    public boolean endOfNoteOff() {
        return state == SynthRipperStateEnum.NOTE_OFF && (durationInSec > maxNoteReleaseDurationSec || isSilentBuffer());
    }

    public boolean endOfNoteOn() {
        return state == SynthRipperStateEnum.NOTE_ON_START && (durationInSec > maxNoteDurationSec || isSilentBuffer());
    }

    public boolean soundDetected() {
        return state == SynthRipperStateEnum.NOTE_ON_SEND && !isSilentBuffer();
    }

    public boolean endOfIdle() {
        return state == SynthRipperStateEnum.IDLE && durationInSec > 1;
    }

    public boolean endOfInit() {
        return state == SynthRipperStateEnum.INIT && durationInSec > 1;
    }

    public boolean acquireNoiseFloor() {
        return state == SynthRipperStateEnum.ACQUIRE_NOISE_FLOOR;
    }

    public boolean endOfAcquireNoiseFloor() {
        return state == SynthRipperStateEnum.ACQUIRE_NOISE_FLOOR && durationInSec > NOISE_FLOOR_CAPTURE_DURATION_IN_SEC;
    }

    public boolean endOfNoteRecord() {
        return state == SynthRipperStateEnum.NOTE_OFF_DONE;
    }

}
