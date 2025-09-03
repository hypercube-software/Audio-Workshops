package com.hypercube.workshop.midiworkshop.clock;

import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.clock.MidiClockType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiClockCLI {
    private final MidiClockExample clock;


    @ShellMethod(value = "Send a MIDI Clock at a given tempo")
    public void clock(@ShellOption(value = "-o") String outputDevice, @ShellOption(value = "-c") MidiClockType clockType, @ShellOption(value = "-t") int tempo) throws IOException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        try (var out = m.openOutput(outputDevice)) {
            clock.startClock(clockType, out, tempo);
        }
    }
}
