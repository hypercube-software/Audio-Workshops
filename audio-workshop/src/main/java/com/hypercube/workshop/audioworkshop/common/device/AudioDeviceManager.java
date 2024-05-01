package com.hypercube.workshop.audioworkshop.common.device;

import lombok.Getter;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.Arrays;
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

            Line.Info[] inputLinesInfo = m.getTargetLineInfo();
            Line.Info[] outputLinesInfo = m.getSourceLineInfo();
            if (inputLinesInfo.length > 0) {
                var inputLines = Arrays.stream(inputLinesInfo)
                        .filter(l -> l instanceof DataLine.Info)
                        .map(l -> (DataLine.Info) l)
                        .toList();
                if (!inputLines.isEmpty()) {
                    inputs.add(new AudioInputDevice(mixerInfo, inputLines));
                }
            } else if (outputLinesInfo.length > 0) {
                var outputLines = Arrays.stream(outputLinesInfo)
                        .filter(l -> l instanceof DataLine.Info)
                        .map(l -> (DataLine.Info) l)
                        .toList();
                if (!outputLines.isEmpty()) {
                    outputs.add(new AudioOutputDevice(mixerInfo, outputLines));
                }
            }
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
