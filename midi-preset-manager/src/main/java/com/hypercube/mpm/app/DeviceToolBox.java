package com.hypercube.mpm.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.mpm.javafx.error.ApplicationError;
import com.hypercube.mpm.javafx.widgets.dialog.generic.GenericDialogController;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetConsumer;
import com.hypercube.workshop.midiworkshop.api.presets.generic.MidiPresetCrawler;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceBankMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceDefinitionMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.mixin.MidiDeviceModeMixin;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.serializer.MidiDevicePresetSerializer;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceToolBox {
    private final MidiDeviceLibrary midiDeviceLibrary;
    private final MidiPortsManager midiPortsManager;

    private static File getBankFolder(String deviceMode, String bankName, MidiDeviceDefinition midiDeviceDefinition) {
        String fullPath = "%s/%s/%s/%s".formatted(
                midiDeviceDefinition.getDefinitionFile()
                        .getParentFile()
                        .getAbsolutePath(),
                midiDeviceDefinition.getDeviceName(),
                deviceMode,
                bankName);
        return new File(fullPath);
    }

    public static void deleteBankFolder(File bankFolder) {
        if (bankFolder.exists()) {
            try (Stream<Path> walk = Files.walk(bankFolder.toPath())) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.error("Unable to delete {}", p.getFileName());
                            }
                        });
            } catch (IOException e) {
                log.error("Unable to inspect folder {}", bankFolder.toString());
            }
        }
    }

    public void dumpPresets(String deviceName, MidiPresetConsumer midiPresetConsumer) {
        MidiPresetCrawler midiPresetCrawler = new MidiPresetCrawler();
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.addMixIn(MidiDeviceBank.class, MidiDeviceBankMixin.class);
        mapper.addMixIn(MidiDeviceDefinition.class, MidiDeviceDefinitionMixin.class);
        mapper.addMixIn(MidiDeviceMode.class, MidiDeviceModeMixin.class);
        SimpleModule module = new SimpleModule();
        module.addSerializer(MidiDevicePreset.class, new MidiDevicePresetSerializer());
        mapper.registerModule(module);
        try (PrintWriter out = new PrintWriter(new FileOutputStream("%s-presets.yml".formatted(deviceName)))) {
            MidiDeviceDefinition devicePresets = new MidiDeviceDefinition();
            midiPresetCrawler.crawlAllPatches(deviceName, (device, midiPreset, currentCount, totalCount) -> {
                devicePresets.setDeviceName(device.getDeviceName());
                devicePresets.setBrand(device.getBrand());
                String modeName = midiPreset.getId()
                        .deviceMode();
                String bankName = midiPreset.getId()
                        .bankName();
                MidiDeviceMode mode = devicePresets.getDeviceModes()
                        .get(modeName);
                if (mode == null) {
                    mode = new MidiDeviceMode();
                    mode.setName(modeName);
                    devicePresets.getDeviceModes()
                            .put(modeName, mode);
                }
                MidiDeviceBank bank = mode.getBank(bankName)
                        .orElse(null);
                if (bank == null) {
                    bank = new MidiDeviceBank();
                    bank.setName(bankName);
                    mode.getBanks()
                            .put(bankName, bank);
                }
                String command = midiPreset.getCommand();
                List<String> drumMap = midiPreset.getDrumKitNotes()
                        .stream()
                        .map(drumKitNote -> "%02X | %s".formatted(drumKitNote.note(), drumKitNote.title()))
                        .toList();
                MidiDevicePreset midiDevicePreset = new MidiDevicePreset(null, midiPreset.getId()
                        .name(), command, midiPreset.getId()
                        .category(), "", drumMap);
                bank.getPresets()
                        .add(midiDevicePreset);
                midiPresetConsumer.onNewMidiPreset(device, midiPreset, currentCount, totalCount);
            });
            mapper.writeValue(out, devicePresets);
        } catch (IOException e) {
            throw new ApplicationError("Unexpected error dumping presets from device '%s'".formatted(deviceName), e);
        }
    }

    public Optional<byte[]> request(MidiDeviceDefinition device, String text, Consumer<RequestStatus> requestLogger) {
        try {
            CommandMacro cmd = CommandMacro.parse(CommandMacro.UNSAVED_MACRO, "DeviceToolBoxCommand():" + text);
            CommandCall call = CommandCall.parse(CommandMacro.UNSAVED_MACRO, "DeviceToolBoxCommand()")
                    .getFirst();
            String payload = cmd.expand(call);
            try (var output = midiPortsManager.getOutput(device.getOutputMidiDevice())
                    .orElse(null)) {
                if (output == null) {
                    String msg = "Output MIDI Device not found: %s".formatted(device.getOutputMidiDevice());
                    log.warn(msg);
                    requestLogger.accept(RequestStatus.of(msg));
                    return Optional.empty();
                }
                try (var input = midiPortsManager.getInput(device.getInputMidiDevice())
                        .orElse(null)) {
                    if (input == null) {
                        String msg = "Input MIDI Device not found: %s".formatted(device.getInputMidiDevice());
                        log.warn(msg);
                        requestLogger.accept(RequestStatus.of(msg));
                        return Optional.empty();
                    }
                    try (ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream()) {
                        MidiListener midiListener = (d, event) -> {
                            try {
                                responseBuffer.write(event.getMessage()
                                        .getMessage());
                            } catch (IOException e) {
                                log.error("Error reading event content", e);
                            }
                        };
                        try (ByteArrayOutputStream requestBytes = new ByteArrayOutputStream()) {
                            output.open();
                            input.open();
                            input.addListener(midiListener);
                            var bytesSent = midiDeviceLibrary.sendCommandToDevice(device, output, cmd, call);
                            requestBytes.write(bytesSent);
                            requestLogger.accept(RequestStatus.of(requestBytes.toByteArray()));
                        }
                        Instant start = Instant.now();
                        while (responseBuffer.size() == 0) {
                            if (Duration.between(start, Instant.now())
                                    .getSeconds() > 3) {
                                log.warn("No response from device {}", device.getDeviceName());
                                requestLogger.accept(RequestStatus.of("No response from device"));
                                break;
                            }
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        input.removeListener(midiListener);
                        return Optional.of(responseBuffer.toByteArray());
                    } catch (IOException e) {
                        requestLogger.accept(RequestStatus.of(e.getMessage()));
                        log.error("Unexpected error", e);
                    } finally {
                    }
                }
            }
        } catch (Exception e) {
            requestLogger.accept(RequestStatus.of(e.getMessage()));
        }
        return Optional.empty();
    }

    public void restoreSysEx(Scene scene) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Restore SysEx");

        fileChooser.getExtensionFilters()
                .addAll(
                        new FileChooser.ExtensionFilter("System Exclusive", "*.syx"),
                        new FileChooser.ExtensionFilter("All", "*.*")
                );

        Window stage = scene.getWindow();

        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                GenericDialogController.info("SysEx restored", """
                        The device have successfully received the SysEx file.
                        """);
            } catch (Exception e) {
                GenericDialogController.error("SysEx not restored", e.getMessage());
            }
        }
    }

    public void saveSysEx(String deviceName, String deviceMode, String bankName, List<MidiPresetCategory> selectedCategories, String patchName) {
        if (bankName == null) {
            GenericDialogController.error("Bank Not Selected", "Select a single bank first before saving patch");
            return;
        }
        if (selectedCategories.isEmpty()) {
            GenericDialogController.error("Categories Not Selected", "Select at least one category before saving patch");
        }
        midiDeviceLibrary.getDevice(deviceName)
                .ifPresent(midiDeviceDefinition -> {
                    String fullPath = "%s/%s/%s/%s/[%s] %s.syx".formatted(
                            midiDeviceDefinition.getDefinitionFile()
                                    .getParentFile()
                                    .getAbsolutePath(),
                            midiDeviceDefinition.getDeviceName(),
                            deviceMode,
                            bankName,
                            selectedCategories.stream()
                                    .map(c -> c.name())
                                    .collect(Collectors.joining(",")), patchName);
                    log.info("Save current preset to " + fullPath);
                });
    }

    public void createBank(String deviceName, String deviceMode, String bankName) {
        midiDeviceLibrary.getDevice(deviceName)
                .ifPresent(midiDeviceDefinition -> {
                    File bankFolder = getBankFolder(deviceMode, bankName, midiDeviceDefinition);
                    bankFolder.mkdirs();
                    midiDeviceLibrary.collectCustomBanksAndPatches(midiDeviceDefinition);
                });
    }

    public void deleteBank(String deviceName, String deviceMode, String bankName) {
        midiDeviceLibrary.getDevice(deviceName)
                .ifPresent(midiDeviceDefinition -> {
                    File bankFolder = getBankFolder(deviceMode, bankName, midiDeviceDefinition);
                    deleteBankFolder(bankFolder);
                    midiDeviceLibrary.collectCustomBanksAndPatches(midiDeviceDefinition);
                });
    }
}
