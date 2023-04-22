package com.hypercube.workshop.audioworkshop.common.vca;

public interface Envelope {
    public double value(double x);

    public void reset();

    public boolean done();
}
