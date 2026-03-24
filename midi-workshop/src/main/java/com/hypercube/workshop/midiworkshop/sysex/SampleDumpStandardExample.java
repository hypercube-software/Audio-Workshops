package com.hypercube.workshop.midiworkshop.sysex;

import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.sds.SampleDumpStandard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleDumpStandardExample {
    private final MidiDeviceLibrary library;
    private final MidiDeviceRequester midiDeviceRequester;
    private final MidiPortsManager midiPortsManager;

    public void request(String deviceName, int sampleId) {
        library.load(ConfigHelper.getApplicationFolder(this.getClass()));
        midiPortsManager.collectHardwareDevices();
        var device = library.getDevice(deviceName)
                .orElseThrow();
        try (var output = midiPortsManager.getOutput(device.getOutputMidiDevice())
                .orElse(null)) {
            if (output == null) {
                log.error("Output MIDI Device not found: %s".formatted(device.getOutputMidiDevice()));
                return;
            }
            try (var input = midiPortsManager.getInput(device.getInputMidiDevice())
                    .orElse(null)) {
                if (input == null) {
                    log.error("Input MIDI Device not found: %s".formatted(device.getInputMidiDevice()));
                    return;
                }
                output.open();
                input.open();

                SampleDumpStandard sds = new SampleDumpStandard(device, input, output);
                try {
                    //sds.dumpSample("SampleDumpRespones.syx");
                    if (true) {
                        sds.requestSample(0, sampleId);
                    }
                    if (false) {
                        sds.sendSample(0, sampleId, Files.readAllBytes(Path.of("input.pcm")));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
