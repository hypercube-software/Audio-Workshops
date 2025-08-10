package com.hypercube.workshop.audioworkshop.api.consumer;

public interface SampleConsumer {
    void onSample(double sample, double rms, double[] autoCorrelation, int channel);
}
