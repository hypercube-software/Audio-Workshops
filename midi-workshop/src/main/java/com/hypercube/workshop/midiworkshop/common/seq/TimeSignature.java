package com.hypercube.workshop.midiworkshop.common.seq;

public record TimeSignature(int numerator, int denominator) {
    public int beatDurationInPPQ(int sequencerPPQ) {
        return 4 * sequencerPPQ / denominator;
    }

    @Override
    public String toString() {
        return String.format("%d/%d", numerator, denominator);
    }
}
