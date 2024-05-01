package com.hypercube.workshop.audioworkshop.synth.vca;

import lombok.Getter;

@Getter
public class LinearInterpolator implements Envelope {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    private boolean done = false;

    public LinearInterpolator(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double value(double x) {
        done = (x >= x2);
        return ((x2 - x) * y1 / (x2 - x1)) + ((x - x1) * y2 / (x2 - x1));
    }

    @Override
    public void reset() {
        done = false;
    }

    @Override
    public boolean done() {
        return done;
    }

}
