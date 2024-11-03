package com.hypercube.workshop.audioworkshop.common.pcm;

import java.nio.ByteBuffer;

@FunctionalInterface
public interface SampleToPCMFunction {
    /**
     * Convert a multichannel buffer of samples into a PCM buffer of bytes
     * <p>NOTE: the output buffer is rewinded on each call, you don't need to do it by yourself</p>
     *
     * @param normalizedInput samples
     * @param pcmBuffer       PCM buffer to write to
     * @param nbSamples       how many samples to convert
     * @param nbChannels      how many channels in input buffer
     */
    void convert(double[][] normalizedInput, ByteBuffer pcmBuffer, int nbSamples, int nbChannels);
}
