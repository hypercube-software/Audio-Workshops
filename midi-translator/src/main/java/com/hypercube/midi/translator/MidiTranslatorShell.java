package com.hypercube.midi.translator;

import com.hypercube.midi.translator.config.MidiTranslatorConfiguration;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent
@Slf4j
@AllArgsConstructor
public class MidiTranslatorShell {
    private final MidiTranslator midiTranslator;
    private final MidiTranslatorConfiguration configuration;

    @ShellMethod(value = "Read MIDI input and send to another MIDI output limiting the throughput")
    public void translate() throws IOException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        String inputMidiDevice = configuration.getDevices()
                .getInputMidiDevice();
        String outputMidiDevice = configuration.getDevices()
                .getOutputMidiDevice();
        if (m.getInput(inputMidiDevice)
                .isEmpty()) {
            log.error("Input device not found:" + inputMidiDevice);
            list();
            return;
        }
        if (m.getOutput(outputMidiDevice)
                .isEmpty()) {
            log.error("Output device not found:" + outputMidiDevice);
            list();
            return;
        }
        try (var in = m.openInput(inputMidiDevice)) {

            try (var out = m.openOutput(outputMidiDevice)) {
                midiTranslator.translate(in, out, configuration.getDevices()
                        .getOutputBandwidth());
            }
        }
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
