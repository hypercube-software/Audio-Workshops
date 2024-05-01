package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.common.device.AudioDeviceManager;
import com.hypercube.workshop.audioworkshop.common.errors.AudioError;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class SynthRipperCLI {
    @ShellMethod(value = "Record audio input")
    public void rip(@ShellOption(value = "-c") File configFile) {

        SynthRipperConfiguration cfg = SynthRipperConfiguration.loadConfig(configFile);

        MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
        midiDeviceManager.collectDevices();
        midiDeviceManager.getOutputs()
                .forEach(d -> log.info(d.getName()));

        AudioDeviceManager audioDeviceManager = new AudioDeviceManager();
        audioDeviceManager.collectDevices();
        audioDeviceManager.getInputs()
                .forEach(d -> d.logFormats());
        audioDeviceManager.getOutputs()
                .forEach(d -> d.logFormats());

        AudioFormat format = cfg.getAudio()
                .getFormat();

        var audioInputDevice = audioDeviceManager.getInput(cfg.getDevices()
                        .getInputAudioDevice())
                .orElseThrow(() -> new AudioError("Device not found:" + cfg.getDevices()
                        .getInputAudioDevice()));
        var audioOutputDevice = audioDeviceManager.getOutput(cfg.getDevices()
                        .getOutputAudioDevice())
                .orElseThrow(() -> new AudioError("Device not found:" + cfg.getDevices()
                        .getOutputAudioDevice()));
        var midiOutDevice = midiDeviceManager.getOutput(cfg.getDevices()
                        .getOutputMidiDevice())
                .orElseThrow(() -> new MidiError("Device not found:" + cfg.getDevices()
                        .getOutputMidiDevice()));
        SynthRipper synthRecorder = null;
        try {
            synthRecorder = new SynthRipper(cfg, format);
            synthRecorder.recordSynth(audioInputDevice, audioOutputDevice, midiOutDevice);
        } catch (IOException e) {
            throw new AudioError(e);
        }

    }
}
