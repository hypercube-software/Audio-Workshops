package com.hypercube.workshop.synthripper.loop;

import com.hypercube.workshop.synthripper.model.LoopSetting;

/**
 * Naive loop detector: the loop is a hard cut of a fixed duration
 * ending at the note off marker.
 */
public class HardCutLoopDetector implements LoopDetector {
    /**
     * Fixed duration of the loop, in seconds
     */
    public static final float LOOP_DURATION_IN_SEC = 3.0f;

    @Override
    public LoopSetting detectLoop(LoopDetectionContext context) {
        LoopSetting loopSetting = new LoopSetting();
        loopSetting.setSampleStart(context.getNoteOffSampleMarker() - (long) (LOOP_DURATION_IN_SEC * context.getSampleRate()));
        loopSetting.setSampleEnd(context.getNoteOffSampleMarker());
        return loopSetting;
    }
}
