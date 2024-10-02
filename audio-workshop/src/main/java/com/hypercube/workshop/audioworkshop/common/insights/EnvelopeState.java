package com.hypercube.workshop.audioworkshop.common.insights;

import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import lombok.Getter;


public class EnvelopeState {
    private final PCMFormat format;
    private final double releaseTimeMs;
    private final double windowSizeInMs;
    private final int releaseTimeInSamples;
    private final int windowSizeInSamples;
    private final int corrWindowSizeInSamples;
    private int sampleCount;
    private double[] rmsAccumulator;
    private double[] rms;
    private double[] prevSamples;
    @Getter
    private double[] correlation;
    private int rmsRelease;

    public EnvelopeState(PCMFormat format, double releaseTimeMs, double windowSizeInMs) {
        this.format = format;
        this.releaseTimeMs = releaseTimeMs;
        this.windowSizeInMs = windowSizeInMs;
        this.releaseTimeInSamples = format.millisecondsToSamples(releaseTimeMs);
        this.windowSizeInSamples = format.millisecondsToSamples(windowSizeInMs);
        this.rmsAccumulator = new double[windowSizeInSamples];
        this.rms = new double[windowSizeInSamples];
        this.prevSamples = new double[windowSizeInSamples];
        this.corrWindowSizeInSamples = 10;
        this.correlation = new double[corrWindowSizeInSamples];
        reset();
    }

    public void onNewSample(double sample, int channel) {
        rmsAccumulator[channel] += sample * sample;
        // shift on the right
        for (int i = windowSizeInSamples - 1; i > 0; i--) {
            prevSamples[i] = prevSamples[i - 1];
        }
        // new sample on the left
        prevSamples[0] = sample;
        // correlation
        for (int delay = 0; delay < corrWindowSizeInSamples; delay++) {
            correlation[delay] = sample - prevSamples[delay];
        }
    }

    public double getRMS(int channel) {
        return rms[channel];
    }

    public void nextSample() {
        sampleCount++;
        if (sampleCount == windowSizeInSamples) {
            for (int c = 0; c < format.getNbChannels(); c++) {
                double currentRms = (double) Math.sqrt(rmsAccumulator[c] / windowSizeInSamples);
                rms[c] = currentRms;
                /*
                if (rms[c] < currentRms || rmsRelease <= 0) {
                    rms[c] = currentRms;
                    rmsRelease = releaseTimeInSamples;
                } else {
                    rmsRelease--;
                }*/
            }
            reset();
        }
    }

    private void reset() {
        for (int c = 0; c < format.getNbChannels(); c++) {
            rmsAccumulator[c] = 0;
        }
        sampleCount = 0;
    }

    public double getPrevSample(int delay) {
        return prevSamples[delay];
    }
}
