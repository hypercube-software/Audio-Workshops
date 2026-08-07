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
}
