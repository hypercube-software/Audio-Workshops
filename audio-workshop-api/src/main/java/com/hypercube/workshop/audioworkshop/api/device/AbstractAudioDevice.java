package com.hypercube.workshop.audioworkshop.api.device;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.sound.sampled.Mixer;

@AllArgsConstructor
@Getter
public class AbstractAudioDevice {
    protected final Mixer.Info mixerInfo;

    public String getName() {
        return mixerInfo.getName();
    }

}
