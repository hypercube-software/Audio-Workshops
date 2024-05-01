package com.hypercube.workshop.audioworkshop.synth.vca;

public class SimpleVCA extends VCA {
    private double currentGain = 0.f;

    private final double defaultGain;

    public SimpleVCA(double sampleRate, double defaultGainInDb) {
        super(sampleRate);
        defaultGain = dbToAmplitude(defaultGainInDb);
        currentGain = defaultGain;
    }

    @Override
    public double getCurrentGain() {
        return currentGain;
    }

    @Override
    public void onNoteOn(double velocity) {
        currentGain = defaultGain;
    }

    @Override
    public void onNoteOff() {
        currentGain = 0;
    }
}
