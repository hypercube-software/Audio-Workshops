package com.hypercube.workshop.audioworkshop.common;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.List;

public class AudioDeviceManager {
    List<AudioInputDevice> inputs = new ArrayList<>();
    List<AudioOutputDevice> outputs = new ArrayList<>();
    public void collectDevices(){
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers){
            Mixer m = AudioSystem.getMixer(mixerInfo);

            Line.Info[] lines = m.getTargetLineInfo();
            if (lines.length>0)
                inputs.add(new AudioInputDevice(mixerInfo));
            else
                outputs.add(new AudioOutputDevice(mixerInfo));
        }
    }

    public List<AudioInputDevice> getInputs() {
        return inputs;
    }

    public List<AudioOutputDevice> getOutputs() {
        return outputs;
    }
}
