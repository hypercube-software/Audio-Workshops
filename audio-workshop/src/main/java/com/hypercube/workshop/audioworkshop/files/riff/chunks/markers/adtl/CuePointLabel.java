package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl;

public record CuePointLabel(
        /**
         * Identifier of the cue point start for 1, not 0
         */
        int dwIdentifier,
        String label) {
}
