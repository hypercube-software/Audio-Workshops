package com.hypercube.workshop.midiworkshop.clock;

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
public class MidiClockCLI {
    private final MidiClockExample clock;


    @ShellMethod(value = "Send a MIDI Clock at a given tempo")
    public void clock(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") MidiClockType clockType, @ShellOption(value = "-t") int tempo) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getOutput(outputDevice)
                .ifPresentOrElse(out -> clock.startClock(clockType, out, tempo), () -> log.error("Output Device not found " + outputDevice));
    }
}
