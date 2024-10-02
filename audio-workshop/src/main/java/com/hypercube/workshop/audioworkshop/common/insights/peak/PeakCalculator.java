package com.hypercube.workshop.audioworkshop.common.insights.peak;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.common.format.PCMFormat;
import lombok.Getter;

@Getter
public class PeakCalculator implements SampleBufferConsumer {
    private double sampleMin;
    private double sampleMax;
    private double samplePeak;
    private double lastSample;
    private long firstZeroCrossingPosInSample;

    @Override
    public void reset() {
        sampleMin = 0;
        sampleMax = 0;
        samplePeak = 0;
        lastSample = 0;
        firstZeroCrossingPosInSample = 0;
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        firstZeroCrossingPosInSample = 0;
        for (int s = 0; s < nbSamples; s++) {
            for (int c = 0; c < nbChannels; c++) {
                double sample = samples[c][s];
                samplePeak = Math.max(samplePeak, Math.abs(sample));
                sampleMin = Math.min(sampleMin, sample);
                sampleMax = Math.max(sampleMax, sample);
                if (firstZeroCrossingPosInSample == 0 && (sample == 0 || Math.signum(sample) * Math.signum(lastSample) == -1)) {
                    firstZeroCrossingPosInSample = s;
                }
                lastSample = sample;
            }
        }
    }

    public double getSamplePeakDb() {
        return PCMFormat.toDb(samplePeak);
    }

    public double getSamplePeakDb(int precision) {
        return withPrecision(getSamplePeakDb(), precision);
    }
}
