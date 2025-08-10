package com.hypercube.workshop.audioworkshop.api.insights.dft.windows;

public class BlackmanHarris implements DFTWindowGenerator {
    @Override
    public double[] generate(int windowsSizeInSamples) {
        double[] window = new double[windowsSizeInSamples];
        double a0 = 0.35875;
        double a1 = 0.48829;
        double a2 = 0.14128;
        double a3 = 0.01168;
        double a1PI = Math.PI * 2;
        double a2PI = Math.PI * 4;
        double a3PI = Math.PI * 6;
        for (int s = 0; s < windowsSizeInSamples; s++) {
            double scale = s / (double) windowsSizeInSamples;
            window[s] = a0 - a1 * Math.cos(a1PI * scale)
                    + a2 * Math.cos(a2PI * scale)
                    - a3 * Math.cos(a3PI * scale);
        }
        return window;
    }
}
