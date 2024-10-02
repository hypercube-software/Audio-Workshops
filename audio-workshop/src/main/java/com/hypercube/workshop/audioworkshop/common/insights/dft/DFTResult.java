package com.hypercube.workshop.audioworkshop.common.insights.dft;

import lombok.Getter;

@Getter
public class DFTResult {
    private final int nbBin;
    private double[] magnitudes;

    public DFTResult(int nbBin) {
        this.nbBin = nbBin;
        this.magnitudes = new double[nbBin];
    }

    public void setMagnitude(int bin, double magnitude) {
        magnitudes[bin] = magnitude;
    }
}
