package com.hypercube.workshop.audioworkshop.common.consumer;

public interface SampleBufferConsumer {
    default void reset() {
        // DO NOTHING BY DEFAULT
    }

    default double avg(double[] values) {
        double dcOffset = 0;
        for (int c = 0; c < values.length; c++) {
            dcOffset += values[c];
        }
        dcOffset /= values.length;
        return dcOffset;
    }

    default double withPrecision(double value, int precision) {
        double p = Math.pow(10, precision);
        return Math.ceil(value * p) / p;
    }

    void onBuffer(double[][] samples, int nbSamples, int nbChannels);
}
