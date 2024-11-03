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

import java.io.File;
import java.io.IOException;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class SynthRipperCLI {

    public static final String DEVICE_NOT_FOUND = "Device not found:";
    private final SynthRipper synthRipper;

    @ShellMethod(value = "Get device infos")
    public void info(@ShellOption(value = "-v", defaultValue = "false") boolean verbose) {
        log.info("Available devices:");
        MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
        midiDeviceManager.collectDevices();
        midiDeviceManager.getOutputs()
                .forEach(d -> log.info("MIDI OUT : " + d.getName()));

        AudioDeviceManager audioDeviceManager = new AudioDeviceManager();
        audioDeviceManager.collectDevices();
        audioDeviceManager.getInputs()
                .forEach(d -> {
                    if (verbose) {
                        d.logFormats();
                    } else {
                        log.info("AUDIO IN : " + d.getName());
                    }
                });
        audioDeviceManager.getOutputs()
                .forEach(d -> {
                    if (verbose) {
                        d.logFormats();
                    } else {
                        log.info("AUDIO OUT: " + d.getName());
                    }
                });
    }

    @ShellMethod(value = "Record audio input")
    public void rip(@ShellOption(value = "-c") File configFile) {

        SynthRipperConfiguration cfg = SynthRipperConfiguration.loadConfig(configFile);
        cfg.getMidi()
                .getSelectedPresets();
        MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
        midiDeviceManager.collectDevices();

        AudioDeviceManager audioDeviceManager = new AudioDeviceManager();
        audioDeviceManager.collectDevices();

        var audioInputDevice = audioDeviceManager.getInput(cfg.getDevices()
                        .getInputAudioDevice())
                .orElseThrow(() -> new AudioError(DEVICE_NOT_FOUND + cfg.getDevices()
                        .getInputAudioDevice()));
        var audioOutputDevice = audioDeviceManager.getOutput(cfg.getDevices()
                        .getOutputAudioDevice())
                .orElseThrow(() -> new AudioError(DEVICE_NOT_FOUND + cfg.getDevices()
                        .getOutputAudioDevice()));
        var midiOutDevice = midiDeviceManager.getOutput(cfg.getDevices()
                        .getOutputMidiDevice())
                .orElseThrow(() -> new MidiError(DEVICE_NOT_FOUND + cfg.getDevices()
                        .getOutputMidiDevice()));
        try {
            synthRipper.init(cfg);
            synthRipper.recordSynth(audioInputDevice, audioOutputDevice, midiOutDevice);
        } catch (IOException e) {
            midiOutDevice.sendAllOff();
            throw new AudioError(e);
        }

    }
}
