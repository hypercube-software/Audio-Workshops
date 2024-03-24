package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MultiMidiInDevice;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiMonitorCLI {
    private final MidiMonitor midiMonitor;

    @ShellMethod(value = "Monitor MIDI input")
    public void monitor(@ShellOption(value = "-i", defaultValue = "") String inputDevice) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        if (!inputDevice.isEmpty()) {
            m.getInput(inputDevice)
                    .ifPresentOrElse(midiMonitor::monitor, () -> log.error("Input Device not found " + inputDevice));
        } else {
            midiMonitor.monitor(new MultiMidiInDevice(m.getInputs()));
        }

    }

    @ShellMethod(value = "Read MIDI input and send to another MIDI output")
    public void filter(@ShellOption(value = "-i", defaultValue = "") String inputDevice, @ShellOption(value = "-o") String outputDevice) {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        if (!inputDevice.isEmpty()) {
            m.getInput(inputDevice)
                    .ifPresentOrElse(in ->
                            filter(in, outputDevice, m), () -> log.error("Input Device not found " + inputDevice));
        } else {
            // Beware of infinite loop backs ! we retire the outputDevice from the list of inputDevices
            filter(new MultiMidiInDevice(m.getInputs()
                    .stream()
                    .filter(d -> !d.getName()
                            .equals(outputDevice))
                    .toList()), outputDevice, m);
        }
    }

    private void filter(MidiInDevice in, String outputDevice, MidiDeviceManager m) {
        m.getOutput(outputDevice)
                .ifPresentOrElse(out -> midiMonitor.filter(in, out), () -> log.error("Output Device not found " + outputDevice));
    }

    @ShellMethod(value = "List MIDI devices")
    public void list() {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("INPUT  Device \"%s\"", d.getName())));
        m.getOutputs()
                .forEach(d -> log.info(String.format("OUTPUT Device \"%s\"", d.getName())));
    }

}
