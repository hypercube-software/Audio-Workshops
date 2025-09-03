package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.util.List;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiMonitorCLI {
    private final MidiMonitor midiMonitor;

    @ShellMethod(value = "Monitor MIDI input")
    public void monitor(@ShellOption(value = "-i", defaultValue = "") String inputDevice) throws IOException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        if (!inputDevice.isEmpty()) {
            try (var in = m.openInput(inputDevice)) {
                midiMonitor.monitor(in);
            }
        } else {
            try (var in = m.openInputs(m.getInputs())) {
                midiMonitor.monitor(in);
            }
        }

    }

    @ShellMethod(value = "Read MIDI input and send to another MIDI output")
    public void filter(@ShellOption(value = "-i", defaultValue = "") String inputDevice, @ShellOption(value = "-o") String outputDevice) throws IOException {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        if (!inputDevice.isEmpty()) {
            try (var in = m.openInput(inputDevice)) {
                try (var out = m.openOutput(outputDevice)) {
                    midiMonitor.filter(in, out);
                }
            }
        } else {
            // Beware of infinite loop backs ! we retire the outputDevice from the list of inputDevices
            List<MidiInDevice> inputDevices = m.getInputs()
                    .stream()
                    .filter(d -> !d.getName()
                            .equals(outputDevice))
                    .toList();
            try (var in = m.openInputs(inputDevices)) {
                try (var out = m.openOutput(outputDevice)) {
                    midiMonitor.filter(in, out);
                }
            }
        }
    }

    @ShellMethod(value = "List MIDI devices")
    public void list() {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("INPUT  Device \"%s\"", d.getName())));
        m.getOutputs()
                .forEach(d -> log.info(String.format("OUTPUT Device \"%s\"", d.getName())));
    }

}
