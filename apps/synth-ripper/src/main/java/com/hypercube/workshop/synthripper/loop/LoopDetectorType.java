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
    AUTOCORRELATION,
    /**
     * Detector based on spectral (frequency domain) similarity at the loop seam.
     */
    FREQUENCY_DOMAIN,
    /**
     * Detector based on the amplitude envelope (ADSR): the loop goes from the
     * start of the Decay (attack peak) to the end of the Sustain (note off).
     */
    ADSR;

    public LoopDetector create() {
        return create(false);
    }

    public LoopDetector create(boolean writeCsv) {
        return switch (this) {
            case HARD_CUT -> new HardCutLoopDetector();
            case AUTOCORRELATION -> new AutocorrelationLoopDetector(writeCsv);
            case FREQUENCY_DOMAIN -> new FrequencyDomainLoopDetector(writeCsv);
            case ADSR -> new AdsrLoopDetector(writeCsv);
        };
    }
}
