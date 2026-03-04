package com.hypercube.workshop.audioworkshop.api.pcm;

public record PCMMarker(String label, long samplePosition) {
    public static PCMMarker of(String label, long samplePosition) {
        return new PCMMarker(label, samplePosition);
    }
}
