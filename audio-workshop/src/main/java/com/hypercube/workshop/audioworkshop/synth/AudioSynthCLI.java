package com.hypercube.workshop.audioworkshop.synth;

import com.hypercube.workshop.audioworkshop.common.AudioDeviceManager;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Slf4j
@ShellComponent
@AllArgsConstructor
public class AudioSynthCLI {
    private final AudioSynth audioSynth;

    @ShellMethod(value = "Play a sine  in real time")
    public void synth(@ShellOption(value = "-i") String inputDevice, @ShellOption(value = "-o") String outputDevice) {
        var audioMgr = new AudioDeviceManager();
        var midiMgr = new MidiDeviceManager();
        audioMgr.collectDevices();
        midiMgr.collectDevices();
        midiMgr.getInput(inputDevice)
                .ifPresentOrElse(midi -> audioMgr.getOutputs()
                        .stream()
                        .filter(d -> d.getName()
                                .equals(outputDevice))
                        .findFirst()
                        .ifPresentOrElse(audio -> audioSynth.synth(midi, audio),
                                () -> log.error("Audio Device not found:" + outputDevice)), () -> log.error("Midi Device not found " + inputDevice));

    }

    @ShellMethod(value = "List Audio devices")
    public void list() {
        AudioDeviceManager m = new AudioDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("AUDIO INPUT  Device \"%s\"", d.getName())));
        m.getOutputs()
                .forEach(d -> log.info(String.format("AUDIO OUTPUT Device \"%s\"", d.getName())));

        MidiDeviceManager midi = new MidiDeviceManager();
        midi.collectDevices();
        midi.getInputs()
                .forEach(d -> log.info(String.format("MIDI INPUT  Device \"%s\"", d.getName())));
        midi.getOutputs()
                .forEach(d -> log.info(String.format("MIDI OUTPUT Device \"%s\"", d.getName())));
    }
}
