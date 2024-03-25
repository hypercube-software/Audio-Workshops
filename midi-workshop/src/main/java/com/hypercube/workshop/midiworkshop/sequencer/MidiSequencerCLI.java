package com.hypercube.workshop.midiworkshop.sequencer;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.clock.MidiClockType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiSequencerCLI {
    private final MidiSequencer sequencer;

    @ShellMethod(value = "Reset all MIDI out devices")
    public void reset() {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutputs()
                .forEach(o -> {
                    log.info("Reset device " + o.getName());
                    o.sendAllOff();
                });
    }

    @ShellMethod(value = "Play something")
    public void elise(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-t") int tempo) throws IOException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.listDevices();
        try (var out = m.openOutput(outputDevice)) {
            sequencer.playResource(out, "midi/for_elise_by_beethoven.mid", tempo);
        }
    }

    @ShellMethod(value = "Play something")
    public void bach(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-t") int tempo) throws IOException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.listDevices();
        try (var out = m.openOutput(outputDevice)) {
            sequencer.playResource(out, "midi/bach_prelude_c_major_846.mid", tempo);
        }
    }

    @ShellMethod(value = "Play something")
    public void play1(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") String clockDevice, @ShellOption(value = "-t") int tempo) throws IOException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.listDevices();
        try (var clock = m.openOutput(clockDevice)) {
            try (var out = m.openOutput(outputDevice)) {
                sequencer.playSequence(MidiClockType.SEQ, clock, out, tempo);
            }
        }
    }

    @ShellMethod(value = "Play something")
    public void play2(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") String clockDevice, @ShellOption(value = "-t") int tempo) throws IOException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.listDevices();
        try (var clock = m.openOutput(clockDevice)) {
            try (var out = m.openOutput(outputDevice)) {
                sequencer.playSequence(MidiClockType.TMR, clock, out, tempo);
            }
        }
    }
}
