package com.hypercube.workshop.synthripper.config;

import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.AudioFormat;

@Getter
@Setter
public class AudioSettings {
    private int nbChannels;
    private int bitDepth;
    private int sampleRate;

    public AudioFormat getFormat() {
        return new AudioFormat(sampleRate, bitDepth, nbChannels, true, false);
    }
}
