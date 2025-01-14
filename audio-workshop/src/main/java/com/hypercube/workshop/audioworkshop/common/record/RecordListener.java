package com.hypercube.workshop.audioworkshop.common.record;

import com.hypercube.workshop.audioworkshop.common.consumer.SampleBuffer;

@FunctionalInterface
public interface RecordListener {
    /**
     * @param buffer incoming audio samples per channel
     * @return true to continue the recording
     */
    boolean onNewBuffer(SampleBuffer buffer, byte[] pcmBuffer, int pcmSize);
}
