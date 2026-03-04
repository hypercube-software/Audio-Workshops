package com.hypercube.workshop.audioworkshop.api.insights.rms;

import com.hypercube.workshop.audioworkshop.api.consumer.SampleBuffer;
import com.hypercube.workshop.audioworkshop.api.consumer.SampleBufferConsumer;
import com.hypercube.workshop.audioworkshop.api.format.PCMFormat;
import lombok.Getter;

public class RMSCalculator implements SampleBufferConsumer {
    private final int nbChannels;
    private long totalSamples;
    private double[] power;
    @Getter
    private double[] rms;

    public RMSCalculator(int nbChannels) {
        this.nbChannels = nbChannels;
        this.power = new double[nbChannels];
        this.rms = new double[nbChannels];
    }

    @Override
    public void reset() {
        for (int c = 0; c < nbChannels; c++) {
            power[c] = 0;
            rms[c] = 0;
        }
        totalSamples = 0;
    }

    @Override
    public void onBuffer(SampleBuffer buffer) {
        totalSamples += buffer.nbSamples();
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < buffer.nbSamples(); s++) {
                var sample = buffer.sample(c, s);
                power[c] += sample * sample;
            }
            rms[c] = Math.sqrt(power[c] / totalSamples);
        }
    }

    public double getRMSDb(RMSReference referenceType) {
        double reference = switch (referenceType) {
            case SINE_WAVE_AES_17 -> 1 / Math.sqrt(2);
            case SQUARE_WAVE -> 1;
            case TRIANGLE_WAVE -> 1 / Math.sqrt(3);
        };
        return PCMFormat.toDb(avg(rms) / reference);
    }

    public double getRMSDb(RMSReference referenceType, int precision) {
        return withPrecision(getRMSDb(referenceType), precision);
    }
}
