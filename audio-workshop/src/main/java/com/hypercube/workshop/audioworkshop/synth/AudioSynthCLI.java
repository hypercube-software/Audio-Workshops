package com.hypercube.workshop.audioworkshop.synth;

import com.hypercube.workshop.audioworkshop.api.device.AudioDeviceManager;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class AudioSynthCLI {
    private final AudioSynth audioSynth;

    @ShellMethod(value = "Play a sine  in real time")
    public void synth(@ShellOption(value = "-i") String inputDevice, @ShellOption(value = "-o") String outputDevice) throws IOException {
        var audioMgr = new AudioDeviceManager();
        audioMgr.collectDevices();

        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        try (var in = m.openInput(inputDevice)) {
            audioMgr.getOutput(outputDevice)
                    .ifPresentOrElse(audio -> audioSynth.synth(in, audio),
                            () -> log.error("Audio Device not found:" + outputDevice));
        }
    }

    @ShellMethod(value = "List Audio devices")
    public void list() {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("AUDIO INPUT  Device \"%s\"", d.getName())));
        m.getOutputs()
                .forEach(d -> log.info(String.format("AUDIO OUTPUT Device \"%s\"", d.getName())));

        MidiPortsManager midi = new MidiPortsManager();
        midi.collectDevices();
        midi.getInputs()
                .forEach(d -> log.info(String.format("MIDI INPUT  Device \"%s\"", d.getName())));
        midi.getOutputs()
                .forEach(d -> log.info(String.format("MIDI OUTPUT Device \"%s\"", d.getName())));
    }
}
