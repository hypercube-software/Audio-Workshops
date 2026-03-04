package com.hypercube.workshop.synthripper.preset;

import com.hypercube.workshop.synthripper.model.RecordedSynthNote;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;

import java.util.List;

public interface PresetGenerator {
    String getAlias();

    void generate(SynthRipperConfiguration conf, List<RecordedSynthNote> sampleBatch);
}
