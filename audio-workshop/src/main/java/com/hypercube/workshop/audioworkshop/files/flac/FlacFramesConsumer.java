package com.hypercube.workshop.audioworkshop.files.flac;

@FunctionalInterface
public interface FlacFramesConsumer {
    void onFrameData(byte[] frames, int size) throws FlacError;
}
