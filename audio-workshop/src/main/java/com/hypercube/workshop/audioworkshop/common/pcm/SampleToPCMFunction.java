package com.hypercube.workshop.audioworkshop.common.pcm;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface SampleToPCMFunction {
    void convert(float[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels);
}