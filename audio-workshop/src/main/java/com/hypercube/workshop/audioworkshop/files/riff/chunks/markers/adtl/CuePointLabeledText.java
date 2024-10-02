package com.hypercube.workshop.audioworkshop.files.riff.chunks.markers.adtl;

public record CuePointLabeledText(
        /**
         * Identifier of the cue point start for 1, not 0
         */
        int dwIdentifier,
        String label,
        int sampleLength,
        String purposeId,
        int countryId,
        int language,
        int dialect,
        int codePage) {
}
