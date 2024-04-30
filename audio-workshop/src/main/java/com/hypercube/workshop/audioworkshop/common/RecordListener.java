package com.hypercube.workshop.audioworkshop.common;

@FunctionalInterface
public interface RecordListener {
    /**
     * @param sampleBuffer incoming audio buffer per channel
     * @param nbSamples    number of samples in each buffer
     * @return false to stop the recording
     */
    boolean onNewBuffer(float[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize);
}
