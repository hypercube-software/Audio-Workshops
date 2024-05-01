package com.hypercube.workshop.synthripper;

public class SynthRipperState {
    float durationInSec;
    int note;
    int lowestNote;
    int highestNote;
    int noteIncrement;
    SynthRipperStateEnum state = SynthRipperStateEnum.IDLE;
}
