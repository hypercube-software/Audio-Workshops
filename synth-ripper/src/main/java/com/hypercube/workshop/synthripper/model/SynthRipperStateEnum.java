package com.hypercube.workshop.synthripper.model;

public enum SynthRipperStateEnum {
    INIT,
    ACQUIRE_NOISE_FLOOR,
    IDLE,
    NOTE_OFF,
    NOTE_OFF_DONE,
    NOTE_ON_SEND,
    NOTE_ON_START
}
