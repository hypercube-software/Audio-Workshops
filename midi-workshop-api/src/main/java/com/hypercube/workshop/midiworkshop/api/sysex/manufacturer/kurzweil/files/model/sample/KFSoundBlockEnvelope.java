package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.sample;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFSoundBlockEnvelope {
    private int attackRate;
    private int attackLevel;
    private int decayRate;
    private int decayLevel;
    private int releaseRate;
    private int releaseLevel;
}
