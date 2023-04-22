package com.hypercube.workshop.audioworkshop.common;

import lombok.AllArgsConstructor;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

@AllArgsConstructor
public class AbstractAudioDevice {
    protected final Mixer.Info mixerInfo;

    public String getName() {
        return mixerInfo.getName();
    }

}
