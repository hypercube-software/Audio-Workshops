package com.hypercube.midi.translator;

import com.hypercube.midi.translator.config.project.ProjectConfiguration;
import com.hypercube.midi.translator.config.project.ProjectConfigurationFactory;
import com.hypercube.midi.translator.config.project.ProjectDevice;
import com.hypercube.midi.translator.model.DeviceInstance;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.devices.AbstractMidiDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ShellComponent("MidiBackupTranslator")
@Slf4j
@AllArgsConstructor
public class MidiBackupTranslatorShell {
    private final ProjectConfigurationFactory projectConfigurationFactory;

    private static void wakeUpDevice(MidiOutDevice out, DeviceInstance device) {
        log.info("Wake up MIDI out device '{}' with ActiveSensing...", out.getName());
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 4000) {
            out.sendActiveSensing();
            device.sleep(out, 200);
        }
    }

    @ShellMethod(value = "Read MIDI input and send to another MIDI output limiting the throughput")
    public void translate() throws IOException {
        final MidiPortsManager m = new MidiPortsManager();
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
    public void restore(@ShellOption(value = "-d", defaultValue = "") String deviceName) throws IOException {
        final MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        final ProjectConfiguration configuration = projectConfigurationFactory.loadConfig();

        for (var projectDevice : configuration.getDevices()) {
            DeviceInstance device = new DeviceInstance(projectDevice);
            if (device.getBackupFile()
                    .exists() && device.isEnabled() && (deviceName == null || projectDevice.getName()
                    .equals(deviceName))) {
                log.info("Restore device: " + projectDevice.getName() + "...");
                try (var out = m.openOutput(projectDevice.getOutputMidiDevice())) {
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
    public void backup(@ShellOption(value = "-d", defaultValue = "") String deviceName, @ShellOption(value = "-m", defaultValue = "") String macro) throws IOException, InvalidMidiDataException, MidiUnavailableException, InterruptedException {
        MidiPortsManager midiPortsManager = new MidiPortsManager();
        midiPortsManager.collectDevices();
        final ProjectConfiguration configuration = projectConfigurationFactory.loadConfig();
        if (deviceName.isBlank() && macro.isBlank()) {
            int nbEnabled = 0;
            for (var projectDevice : configuration.getDevices()) {
                DeviceInstance device = new DeviceInstance(projectDevice);
                if (device.isEnabled()) {
                    nbEnabled++;
                    backupDevice(projectDevice, device, midiPortsManager, null);
                }
            }
            if (nbEnabled == 0) {
                log.warn("No devices enabled for backup. Nothing to do.");
            }
        } else {
            if (deviceName.isBlank() && !macro.isBlank()) {
                log.error("You also need to pass a macro name");
            } else if (!deviceName.isBlank() && macro.isBlank()) {
                log.error("You also need to pass a device name");
            }
            configuration.getDevices()
                    .stream()
                    .forEach(projectDevice -> {
                        DeviceInstance device = new DeviceInstance(projectDevice);
                        if (device.isEnabled()) {
                            backupDevice(projectDevice, device, midiPortsManager, macro);
                        }
                    });
        }
    }

    @ShellMethod(value = "List MIDI devices")
    public void list() {
        MidiPortsManager m = new MidiPortsManager();
        m.collectDevices();
        m.getInputs()
                .forEach(d -> log.info(String.format("MIDI INPUT Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
        m.getOutputs()
                .forEach(d -> log.info(String.format("MIDI OUTPUT Port \"%s\"%s", d.getName(), getDeviceAlias(d))));
    }

    private void backupDevice(ProjectDevice projectDevice, DeviceInstance deviceInstance, MidiPortsManager midiPortsManager, String macro) {
        log.info("-------------------------------------------------------");
        log.info("Backup device: " + projectDevice.getName() + "...");
        log.info("inactivityTimeoutMs : " + deviceInstance.getInactivityTimeoutMs());
        log.info("sysexPauseMs        : " + deviceInstance.getSysexPauseMs());
        log.info("inputMidiDevice     : " + projectDevice.getInputMidiDevice());
        log.info("outputMidiDevice    : " + projectDevice.getOutputMidiDevice());
        log.info("-------------------------------------------------------");
        try (var in = midiPortsManager.openInput(projectDevice.getInputMidiDevice())) {
            in.addSysExListener((midiInDevice, sysExEvent) -> onSysEx(deviceInstance, midiInDevice, sysExEvent));
            in.startListening();
            try (var out = midiPortsManager.openOutput(projectDevice.getOutputMidiDevice())) {
                wakeUpDevice(out, deviceInstance);
                if (macro == null) {
                    for (int requestIndex = 0; requestIndex < projectDevice.getDumpRequests()
                            .size(); requestIndex++) {
                        var requests = projectDevice.getDumpRequestTemplates()
                                .get(requestIndex);
                        sendBulkRequests(projectDevice, deviceInstance, out, requests.getMidiRequests());
                    }
                } else {
                    var midiRequestSequence = projectConfigurationFactory.forgeMidiRequestSequence(projectDevice, macro);
                    sendBulkRequests(projectDevice, deviceInstance, out, midiRequestSequence.getMidiRequests());
                }
            }

            in.stopListening();
            in.waitNotListening();
            deviceInstance.save();
            log.info("{}{}", projectDevice.getName(), " saved 0x%X (%d) bytes".formatted(deviceInstance.getStateSize(), deviceInstance.getStateSize()));
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    @SuppressWarnings("java:S106")
    private void sendBulkRequests(ProjectDevice projectDevice, DeviceInstance device, MidiOutDevice out, List<MidiRequest> midiRequests) throws InvalidMidiDataException {
        for (int requestIndex = 0; requestIndex < projectDevice.getDumpRequests()
                .size(); requestIndex++) {
            var requests = projectDevice.getDumpRequestTemplates()
                    .get(requestIndex);
            for (var request : requests.getMidiRequests()) {
                device.setCurrentRequest(request);
                List<com.hypercube.workshop.midiworkshop.api.CustomMidiEvent> requestInstances = SysExBuilder.parse(request.getValue());
                for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                    var customMidiEvent = requestInstances.get(requestInstanceIndex);
                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValues());
                    device.sendAndWaitResponse(out, customMidiEvent);

                    if (request.getResponseSize() != null && device.getCurrentResponseSize() != request.getResponseSize()) {
                        log.error("Unexpected size received (0x%X) given what you specified (0x%X)".formatted(device.getCurrentResponseSize(), request.getResponseSize()));
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
    private void onSysEx(DeviceInstance device, MidiInDevice midiInDevice, com.hypercube.workshop.midiworkshop.api.CustomMidiEvent customMidiEvent) {
        byte[] data = customMidiEvent.getMessage()
                .getMessage();
        if (device.getCurrentResponseSize() == 0) {
            System.out.print("    Receiving");
        }
        device.addBytes(data);
        System.out.print(".");
        System.out.flush();
    }

    private String getDeviceAlias(AbstractMidiDevice midiDevice) {
        return projectConfigurationFactory.getLibraryDeviceFromMidiPort(midiDevice
                        .getName())
                .map(def -> " => bound to library device \"%s\"".formatted(def.getDeviceName()))
                .orElse("");
    }

}
