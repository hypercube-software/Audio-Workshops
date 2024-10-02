package com.hypercube.workshop.audioworkshop.common.record;

@FunctionalInterface
public interface RecordListener {
    /**
     * @param sampleBuffer incoming audio buffer per channel
     * @param nbSamples    number of samples in each buffer
     * @return false to stop the recording
     */
    boolean onNewBuffer(double[][] sampleBuffer, int nbSamples, byte[] pcmBuffer, int pcmSize);
}
