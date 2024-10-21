package com.hypercube.workshop.audioworkshop.common.record;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;

@FunctionalInterface
public interface RecordListener {
    /**
     * @param buffer incoming audio samples per channel
     * @return false to stop the recording
     */
    boolean onNewBuffer(SampleBuffer buffer, byte[] pcmBuffer, int pcmSize);
}
