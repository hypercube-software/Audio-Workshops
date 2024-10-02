package com.hypercube.workshop.audioworkshop.common.filter.dc;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBufferConsumer;

public class DCOffsetRemover implements SampleBufferConsumer {
    private final double[] dcOffset;
    private final int nbChannels;

    public DCOffsetRemover(double[] dcOffset) {
        this.dcOffset = dcOffset;
        this.nbChannels = dcOffset.length;
    }

    public DCOffsetRemover(double dcOffset, int nbChannels) {
        this.dcOffset = new double[nbChannels];
        this.nbChannels = nbChannels;
        for (int c = 0; c < nbChannels; c++) {
            this.dcOffset[c] = dcOffset;
        }
    }

    @Override
    public void onBuffer(double[][] samples, int nbSamples, int nbChannels) {
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < nbSamples; s++) {
                samples[c][s] -= dcOffset[c];
            }
        }
    }
}
