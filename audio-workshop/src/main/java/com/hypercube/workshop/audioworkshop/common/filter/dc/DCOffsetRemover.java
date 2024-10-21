package com.hypercube.workshop.audioworkshop.common.filter.dc;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;
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
    public void onBuffer(SampleBuffer buffer) {
        for (int c = 0; c < nbChannels; c++) {
            for (int s = 0; s < buffer.nbSamples(); s++) {
                buffer.setSample(c, s, buffer.sample(c, s) - dcOffset[c]);
            }
        }
    }
}
