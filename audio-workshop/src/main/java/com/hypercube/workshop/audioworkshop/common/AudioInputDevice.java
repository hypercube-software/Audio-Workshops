package com.hypercube.workshop.audioworkshop.common;

import javax.sound.sampled.Mixer;

public class AudioInputDevice extends AbstractAudioDevice{

    public AudioInputDevice(Mixer.Info mixerInfo) {
        super(mixerInfo);
    }
}
