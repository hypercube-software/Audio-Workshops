package com.hypercube.workshop.synthripper.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoopSetting {
    /**
     * Start of the loop, in samples (inclusive)
     */
    private long sampleStart;
    /**
     * End of the loop, in samples (exclusive)
     */
    private long sampleEnd;
    /**
     * Natural period of the loop, in samples. This is the length of a single
     * repetition; the final loop {@code [sampleStart, sampleEnd]} may span several
     * periods when it has been extended by {@code loopLength} multiples.
     */
    private long loopLength;
    /**
     * Position (in samples) of the attack peak, i.e. the start of the Decay phase.
     * Only set by the ADSR detector; used to embed ADSR analysis markers in the wav.
     */
    private Long attackPeakSample;
    /**
     * Position (in samples) of the start of the Sustain phase, i.e. the end of the
     * Decay. Only set by the ADSR detector; used to embed ADSR markers in the wav.
     */
    private Long sustainStartSample;
}
