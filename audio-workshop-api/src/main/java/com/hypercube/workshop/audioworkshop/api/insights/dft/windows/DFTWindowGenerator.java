package com.hypercube.workshop.audioworkshop.api.insights.dft.windows;

public interface DFTWindowGenerator {
    double[] generate(int windowsSizeInSamples);
}
