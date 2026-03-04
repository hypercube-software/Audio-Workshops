package com.hypercube.workshop.audioworkshop.api.insights.peak;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
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
    public void onBuffer(SampleBuffer buffer) {
        firstZeroCrossingPosInSample = 0;
        for (int s = 0; s < buffer.nbSamples(); s++) {
            for (int c = 0; c < buffer.nbChannels(); c++) {
                double sample = buffer.sample(c, s);
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
