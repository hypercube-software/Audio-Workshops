package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

/**
 * Detects the sustain loop of a recorded sample.
 * The loop is a segment [start,end] of the sample which, once played
 * repeatedly, reproduces the sustained sound faithfully.
 */
public interface LoopDetector {

    /**
     * Find the loop region of a recorded note.
     *
     * @param context Context holding the recorded samples and markers
     * @return the loop settings (start and end in samples), or {@code null} if no loop is found
     */
    LoopSetting detectLoop(LoopDetectionContext context);
}
