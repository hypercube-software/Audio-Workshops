package com.hypercube.workshop.audioworkshop.common;

import lombok.Getter;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class AudioDeviceManager {
    private final List<AudioInputDevice> inputs = new ArrayList<>();
    private final List<AudioOutputDevice> outputs = new ArrayList<>();

    public void collectDevices() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer m = AudioSystem.getMixer(mixerInfo);

            Line.Info[] lines = m.getTargetLineInfo();
            if (lines.length > 0)
                inputs.add(new AudioInputDevice(mixerInfo));
            else
                outputs.add(new AudioOutputDevice(mixerInfo));
        }
    }

    public Optional<AudioInputDevice> getInput(String deviceName) {
        return inputs
                .stream()
                .filter(d -> d.getName()
                        .equals(deviceName))
                .findFirst();
    }

    public Optional<AudioOutputDevice> getOutput(String deviceName) {
        return outputs
                .stream()
                .filter(d -> d.getName()
                        .equals(deviceName))
                .findFirst();
    }

}
