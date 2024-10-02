package com.hypercube.workshop.audioworkshop.common.insights.dft.windows;

public interface DFTWindowGenerator {
    double[] generate(int windowsSizeInSamples);
}
