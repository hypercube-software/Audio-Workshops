package com.hypercube.workshop.midiworkshop.sequencer;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.clock.MidiClockType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiSequencerCLI {
    private final MidiSequencer sequencer;

    @ShellMethod(value = "Reset all MIDI out devices")
    public void reset() {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutputs().forEach(o -> {
            log.info("Reset device " + o.getName());
            o.sendAllOff();
        });
    }

    @ShellMethod(value = "Play something")
    public void elise(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-t") int tempo) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutput(outputDevice).ifPresentOrElse(out -> sequencer.playResource(out, "midi/for_elise_by_beethoven.mid", tempo), () -> {
            log.error("Output Device not found " + outputDevice);
        });
    }

    @ShellMethod(value = "Play something")
    public void bach(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-t") int tempo) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutput(outputDevice).ifPresentOrElse(out -> sequencer.playResource(out, "midi/bach_prelude_c_major_846.mid", tempo), () -> {
            log.error("Output Device not found " + outputDevice);
        });
    }

    @ShellMethod(value = "Play something")
    public void play1(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") String clockDevice, @ShellOption(value = "-t") int tempo) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutput(clockDevice).ifPresentOrElse(clock -> {
            m.getOutput(outputDevice).ifPresentOrElse(out -> sequencer.playSequence(MidiClockType.SEQ, clock, out, tempo), () -> {
                log.error("Output Device not found " + outputDevice);
            });
        }, () -> {
            log.error("Clock Device not found " + outputDevice);
        });
    }

    @ShellMethod(value = "Play something")
    public void play2(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") String clockDevice, @ShellOption(value = "-t") int tempo) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutput(clockDevice).ifPresentOrElse(clock -> {
            m.getOutput(outputDevice).ifPresentOrElse(out -> sequencer.playSequence(MidiClockType.TMR, clock, out, tempo), () -> {
                log.error("Output Device not found " + outputDevice);
            });
        }, () -> {
            log.error("Clock Device not found " + outputDevice);
        });
    }
}
