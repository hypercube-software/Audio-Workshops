package com.hypercube.workshop.audioworkshop.common.vca;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SustainEnvelope implements Envelope {

    private final double sustain;

    @Override
    public double value(double x) {
        return sustain;
    }

    @Override
    public void reset() {
        // Nothing to do
    }

    @Override
    public boolean done() {
        return false;
    }
}
