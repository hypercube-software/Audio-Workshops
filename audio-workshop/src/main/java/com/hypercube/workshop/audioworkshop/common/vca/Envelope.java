package com.hypercube.workshop.audioworkshop.common.vca;

public interface Envelope {
    double value(double x);

    void reset();

    boolean done();
}
