package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;
import lombok.Builder;
import lombok.Getter;

import java.io.File;

/**
 * Everything a {@link LoopDetector} needs to find the sustain loop.
 * The detector reads the recorded samples from {@link #wavFile}.
 */
@Getter
@Builder
public class LoopDetectionContext {
    /**
     * Sample rate of the recording, in Hz
     */
    private final int sampleRate;
    /**
     * Number of channels of the recording
     */
    private final int nbChannels;
    /**
     * The recorded note wav file
     */
    private final File wavFile;
    /**
     * Marker (in samples) where the note off was sent: the sustain ends there
     */
    private final long noteOffSampleMarker;
    /**
     * Optional pre-detected loop: a {@link LoopDetector} may refine it or return it as is
     */
    private final LoopSetting previousLoopSetting;
}
