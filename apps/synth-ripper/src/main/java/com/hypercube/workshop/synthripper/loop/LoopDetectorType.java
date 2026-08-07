package com.hypercube.workshop.synthripper.loop;

/**
 * Selects which {@link LoopDetector} implementation to use.
 */
public enum LoopDetectorType {
    /**
     * Naive detector: hard cut a fixed number of seconds before the note off marker.
     */
    HARD_CUT,
    /**
     * Detector based on autocorrelation of the sustain region.
     */
    AUTOCORRELATION;

    public LoopDetector create() {
        return switch (this) {
            case HARD_CUT -> new HardCutLoopDetector();
            case AUTOCORRELATION -> new AutocorrelationLoopDetector();
        };
    }
}
