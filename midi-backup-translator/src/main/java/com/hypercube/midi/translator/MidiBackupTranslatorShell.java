package com.hypercube.midi.translator;

import com.hypercube.midi.translator.config.project.ProjectConfiguration;
import com.hypercube.midi.translator.config.project.ProjectConfigurationFactory;
import com.hypercube.midi.translator.config.project.ProjectDevice;
import com.hypercube.midi.translator.model.DeviceInstance;
import com.hypercube.workshop.midiworkshop.common.*;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ShellComponent("MidiBackupTranslator")
@Slf4j
@AllArgsConstructor
public class MidiBackupTranslatorShell {
    private final ProjectConfigurationFactory projectConfigurationFactory;

    @ShellMethod(value = "Read MIDI input and send to another MIDI output limiting the throughput")
    public void translate() throws IOException {
        final MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        final ProjectConfiguration configuration = projectConfigurationFactory.loadConfig();
        final MidiBackupTranslator midiBackupTranslator = new MidiBackupTranslator(configuration);

        if (Optional.ofNullable(configuration.getTranslations())
                .filter(l -> !l.isEmpty())
                .isPresent()) {

            String inputMidiDevice = configuration.getTranslate()
                    .getFromMidiDevice();
            if (m.getInput(inputMidiDevice)
                    .isEmpty()) {
                log.error("Input device '%s' not found for translation".formatted(inputMidiDevice));
                list();
                return;
            }

            String translateToDevice = configuration.getTranslate()
                    .getToDevice();
            ProjectDevice outputDevice = configuration.getDevices()
                    .stream()
                    .filter(d -> d.getName()
                            .equals(translateToDevice))
                    .findFirst()
                    .orElse(projectConfigurationFactory.getDefaultProjectDevice(translateToDevice)
                            .orElse(null));
            if (outputDevice == null) {
                log.error("Output device not found in device list: " + translateToDevice);
                list();
                return;
            }

            String outputMidiDevice = outputDevice.getOutputMidiDevice();
            if (m.getOutput(outputMidiDevice)
                    .isEmpty()) {
                log.error("Output device '%s' not found".formatted(translateToDevice));
                list();
                return;
            }

            try (var in = m.openInput(inputMidiDevice)) {
                try (var out = m.openOutput(outputMidiDevice)) {
                    midiBackupTranslator.translate(in, out, outputDevice.getOutputBandWidth());
                }
            }
        } else {
            log.error("No translations in configuration file");
        }
    }

    @ShellMethod(value = "Restore devices with Sysex")
    public void restore() throws IOException {
        final MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        final ProjectConfiguration configuration = projectConfigurationFactory.loadConfig();

        for (var deviceSetting : configuration.getDevices()) {
            DeviceInstance device = new DeviceInstance(deviceSetting);
            if (device.getBackupFile()
                    .exists() && device.isEnabled()) {
                log.info("Restore device: " + deviceSetting.getName() + "...");
                try (var out = m.openOutput(deviceSetting.getOutputMidiDevice())) {
                    var state = device.loadState();
                    log.info("Send {} SysEx events...", state.size());
                    state.forEach(evt -> {
                        out.send(evt);
                        System.out.print(".");
                        System.out.flush();
                        device.sleep(out, device.getSysexPauseMs());
                    });
                    System.out.println("");
                }
            }
        }
    }

    @ShellMethod(value = "Backup devices with Sysex")
    public void backup() throws IOException, InvalidMidiDataException, MidiUnavailableException, InterruptedException {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        final ProjectConfiguration configuration = projectConfigurationFactory.loadConfig();

        int nbEnabled = 0;
        for (var deviceSetting : configuration.getDevices()) {
            DeviceInstance device = new DeviceInstance(deviceSetting);
            if (device.isEnabled()) {
                nbEnabled++;
                log.info("-------------------------------------------------------");
                log.info("Backup device: " + deviceSetting.getName() + "...");
                log.info("inactivityTimeoutMs : " + device.getInactivityTimeoutMs());
                log.info("sysexPauseMs        : " + device.getSysexPauseMs());
                log.info("inputMidiDevice     : " + deviceSetting.getInputMidiDevice());
                log.info("outputMidiDevice    : " + deviceSetting.getOutputMidiDevice());
                log.info("-------------------------------------------------------");
                try (var in = m.openInput(deviceSetting.getInputMidiDevice())) {
                    in.addSysExListener((midiInDevice, sysExEvent) -> onSysEx(device, midiInDevice, sysExEvent));
                    in.startListening();
                    try (var out = m.openOutput(deviceSetting.getOutputMidiDevice())) {
                        wakeUpDevice(out, device);
                        sendBulkRequests(deviceSetting, device, out);
                    }

                    in.stopListening();
                    in.waitNotListening();
                    device.save();
                    log.info(deviceSetting.getName() + " saved 0x%X (%d) bytes".formatted(device.getStateSize(), device.getStateSize()));
                }
            }
        }
        if (nbEnabled == 0) {
            log.warn("No devices enabled for backup. Nothing to do.");
        }
    }

    private static void wakeUpDevice(MidiOutDevice out, DeviceInstance device) {
        log.info("Wake up MIDI out device '{}' with ActiveSensing...", out.getName());
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 4000) {
            out.sendActiveSensing();
            device.sleep(out, 200);
        }
    }

    @SuppressWarnings("java:S106")
    private void sendBulkRequests(ProjectDevice projectDevice, DeviceInstance device, MidiOutDevice out) throws InvalidMidiDataException {
        for (int requestIndex = 0; requestIndex < projectDevice.getDumpRequests()
                .size(); requestIndex++) {
            var requests = projectDevice.getDumpRequestTemplates()
                    .get(requestIndex);
            for (var request : requests.getMidiRequests()) {
                device.setCurrentRequest(request);
                List<CustomMidiEvent> requestInstances = SysExBuilder.parse(request.getValue());
                for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                    var customMidiEvent = requestInstances.get(requestInstanceIndex);
                    String name = requests.getName()
                            .equals(request.getName()) ? requests.getName() : "%s/%s".formatted(requests.getName(), request.getName());
                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), name, customMidiEvent.getHexValues());
                    device.sendAndWaitResponse(out, customMidiEvent);

                    if (request.getSize() != null && device.getCurrentResponseSize() != request.getSize()) {
                        log.error("Unexpected size received (0x%X) given what you specified (0x%X)".formatted(device.getCurrentResponseSize(), request.getSize()));
                        return;
                    }

                    long receivedBytes = device.getCurrentResponseSize();
                    System.out.println(" 0x%X (%d) bytes".formatted(receivedBytes, receivedBytes));
                    log.debug("Bulk {} {} received: {} bytes", requestIndex + 1, request, receivedBytes);
                    if (receivedBytes == 0) {
                        log.error("No response from device {}", projectDevice.getName());
                        log.error("- Check your bulk request syntax");
                        log.error("- Check your device can receive Sysex");
                        log.error("- Increase 'inactivityTimeoutMs'");
                        return;
                    }
                    device.sleep(out, device.getSysexPauseMs());
                }
            }
        }
    }

    @SuppressWarnings("java:S106")
    private void onSysEx(DeviceInstance device, MidiInDevice midiInDevice, CustomMidiEvent customMidiEvent) {
        MidiMessage msg = customMidiEvent.getMessage();
        SysexMessage sysexMsg = (SysexMessage) msg;

        byte[] data = msg.getStatus() == 0xF0 ? msg.getMessage() : sysexMsg.getData();
        if (device.getCurrentResponseSize() == 0) {
            System.out.print("    Receiving");
        }
        device.addBytes(data);
        System.out.print(".");
        System.out.flush();
    }

    @ShellMethod(value = "List MIDI devices")
    public void list() {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("MIDI INPUT Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
        m.getOutputs()
                .forEach(d -> log.info(String.format("MIDI OUTPUT Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
    }

    private String getDeviceAlias(AbstractMidiDevice midiDevice) {
        return projectConfigurationFactory.getLibraryDeviceFromMidiPort(midiDevice
                        .getName())
                .map(def -> " => bound to library device \"%s\"".formatted(def.getDeviceName()))
                .orElse("");
    }

}
