package com.hypercube.workshop.audioworkshop.common.insights.dc;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBufferConsumer;
import lombok.Getter;

public class DCOffsetCalculator implements SampleBufferConsumer {
    private final int nbChannels;
    private long totalSamples;
    @Getter
    private double[] globalDcOffset;
    private double[] sampleSum;

    public DCOffsetCalculator(int nbChannels) {
        this.nbChannels = nbChannels;
        this.globalDcOffset = new double[nbChannels];
        this.sampleSum = new double[nbChannels];
    }

    @Override
    public void reset() {
        totalSamples = 0;
        for (int c = 0; c < nbChannels; c++) {
            sampleSum[c] = 0;
            globalDcOffset[c] = 0;
        }
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        totalSamples += nbSamples;
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < nbSamples; s++) {
                sampleSum[c] += samples[c][s];
            }
            globalDcOffset[c] = sampleSum[c] / totalSamples;
        }
    }

    public double getDCOffsetPercent(int precision) {
        return withPrecision(avg(globalDcOffset) * 100, precision);
    }


}
