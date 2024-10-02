package com.hypercube.workshop.audioworkshop.common.pcm;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface PCMtoSampleFunction {
    void convert(ByteBuffer pcmBuffer, double[][] normalizedInput, int nbSamples, int nbChannels);
}
