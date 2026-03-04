package com.hypercube.workshop.synthripper;

import com.hypercube.workshop.audioworkshop.api.device.AudioDeviceManager;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.synthripper.config.ConfigFactory;
import com.hypercube.workshop.synthripper.model.SynthRipperError;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.io.IOException;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class SynthRipperCLI {

    public static final String DEVICE_NOT_FOUND = "Device not found:";
    private final SynthRipper synthRipper;
    private final ConfigFactory configFactory;
    private SynthRipperConfiguration config;

    @ShellMethod(value = "Get device infos")
    public void info(@ShellOption(value = "-v", defaultValue = "false") boolean verbose) {
        log.info("Available devices:");
        MidiPortsManager midiPortsManager = new MidiPortsManager();
        midiPortsManager.collectHardwareDevices();
        midiPortsManager.getOutputs()
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
        config = configFactory.loadConfig(configFile);
        config.getSelectedPresets();
        MidiPortsManager midiPortsManager = new MidiPortsManager();
        midiPortsManager.collectHardwareDevices();

        AudioDeviceManager audioDeviceManager = new AudioDeviceManager();
        audioDeviceManager.collectDevices();

        var audioInputDevice = audioDeviceManager.getInput(config.getPorts()
                        .getInputAudioDevice())
                .orElseThrow(() -> new SynthRipperError(DEVICE_NOT_FOUND + config.getPorts()
                        .getInputAudioDevice()));
        var audioOutputDevice = audioDeviceManager.getOutput(config.getPorts()
                        .getOutputAudioDevice())
                .orElseThrow(() -> new SynthRipperError(DEVICE_NOT_FOUND + config.getPorts()
                        .getOutputAudioDevice()));
        var midiOutDevice = midiPortsManager.getOutput(config.getPorts()
                        .getOutputMidiDevice())
                .orElseThrow(() -> new MidiError(DEVICE_NOT_FOUND + config.getPorts()
                        .getOutputMidiDevice()));
        try {
            synthRipper.init(config);
            synthRipper.recordSynth(audioInputDevice, audioOutputDevice, midiOutDevice);
        } catch (IOException e) {
            midiOutDevice.sendAllOff();
            throw new SynthRipperError(e);
        }

    }
}
