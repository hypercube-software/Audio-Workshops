package com.hypercube.workshop.audioworkshop.synth.vca;

public interface Envelope {
    double value(double x);

    void reset();

    boolean done();
}
