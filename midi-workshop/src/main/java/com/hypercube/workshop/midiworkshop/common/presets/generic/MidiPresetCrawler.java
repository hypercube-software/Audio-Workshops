package com.hypercube.workshop.midiworkshop.common.presets.generic;

import com.hypercube.workshop.midiworkshop.MidiWorkshopApplication;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiDeviceTimeout;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.*;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.common.sysex.library.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
public class MidiPresetCrawler {
    private final Pattern SOUND_CANVAS_PRESET_DEFINITION_REGEXP = Pattern.compile("\\d+-\\d+-\\d+\\s(.+)");
    AtomicReference<CustomMidiEvent> currentResponse = new AtomicReference<>();
    int expectedResponseSize = 0;
    ByteArrayOutputStream currentSysex = new ByteArrayOutputStream();

    public void crawlAllPatches(String deviceName, MidiPresetConsumer midiPresetConsumer) {
        MidiDeviceLibrary library = new MidiDeviceLibrary();
        library.load(ConfigHelper.getApplicationFolder(MidiWorkshopApplication.class));
        MidiDeviceDefinition device = library.getDevice(deviceName)
                .orElseThrow(() -> new MidiConfigError("Device not declared in the library: " + deviceName));

        MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
        midiDeviceManager.collectDevices();
        String outputMidiDevice = device.getOutputMidiDevice();
        MidiOutDevice out = midiDeviceManager.getOutput(outputMidiDevice)
                .orElse(null);
        String inputMidiDevice = device.getInputMidiDevice();
        MidiInDevice in = midiDeviceManager.getInput(inputMidiDevice)
                .orElse(null);
        if (out == null) {
            throw new MidiConfigError("MIDI OUT Device not found: '%s".formatted(outputMidiDevice));
        }
        if (in == null) {
            throw new MidiConfigError("MIDI IN Device not found: '%s".formatted(inputMidiDevice));
        }

        try {
            MidiPresetIdentity previousPatchIdentity = null;
            in.open();
            in.addSysExListener(this::onResponse);
            in.startListening();
            out.open();
            for (var mode : device.getDeviceModes()
                    .values()) {
                log.info("Set mode " + mode.getName());
                // no command mean the device switch automatically to the right mode (Like Yamaha TG-500)
                if (mode.getCommand() != null) {
                    MidiRequestSequence setModeRequestSequence = forgeRequestSequence(library, device, mode.getCommand());
                    send(setModeRequestSequence, out);
                    wait("Wait mode change", device.getModeLoadTimeMs());
                }
                MidiRequestSequence modeRequestSequence = forgeRequestSequence(library, device, mode.getQueryName());
                MidiRequestSequence fallbackRequestSequence = mode.getFallBackQueryName() != null ? forgeRequestSequence(library, device, mode.getFallBackQueryName()) : null;
                for (var bank : mode.getBanks()
                        .values()) {
                    if (bank.getPresetDomain() == null) {
                        continue;
                    }
                    MidiRequestSequence bankRequestSequence = bank.getQueryName() != null ? forgeRequestSequence(library, device, bank.getQueryName()) : modeRequestSequence;
                    for (var range : bank.getPresetDomain()
                            .getRanges()) {
                        for (int program : IntStream.rangeClosed(range.getFrom(), range.getTo())
                                .toArray()) {
                            String currentBankName = bank.getName();
                            MidiPreset midiPreset = MidiPresetBuilder.parse(device, mode, bank, program);
                            log.info("Select Bank '%s' Program '%s' in mode '%s'".formatted(currentBankName, program, mode.getName()));
                            int bankLSB = 0;
                            int bankMSB = 0;
                            for (var command : midiPreset.getCommands()) {
                                CustomMidiEvent cm = new CustomMidiEvent(command, 0);
                                log.info("    " + cm.getHexValuesSpaced());
                                if (cm.getHexValues()
                                        .startsWith("0xB020")) {
                                    bankLSB = Integer.parseInt(cm.getHexValues()
                                            .substring(6), 16);
                                }
                                if (cm.getHexValues()
                                        .startsWith("0xB000")) {
                                    bankMSB = Integer.parseInt(cm.getHexValues()
                                            .substring(6), 16);
                                }
                                out.send(cm);
                            }
                            // let the time the edit buffer is completely set before querying it
                            wait("Wait patch change", device.getPresetLoadTimeMs());
                            MidiPresetIdentity midiPresetIdentity = null;
                            for (int retry = 0; retry < 2; retry++) {
                                midiPresetIdentity = switch (device.getPresetNaming()) {
                                    case STANDARD ->
                                            getStandardPreset(mode, currentBankName, bankMSB, bankLSB, program, bankRequestSequence, out);
                                    case SOUND_CANVAS -> getSoundCanvasPreset(device, mode, program, midiPreset);
                                };
                                if (midiPresetIdentity != null) {
                                    if (previousPatchIdentity != null && previousPatchIdentity.name()
                                            .equals(midiPresetIdentity.name())) {
                                        log.error("Something wrong, the patch name is the same than the previous one");
                                        if (fallbackRequestSequence != null) {
                                            log.info("Try with fallback request...");
                                            bankRequestSequence = fallbackRequestSequence;
                                        }
                                    } else {
                                        break;
                                    }
                                } else {
                                    log.error("Something wrong, the patch name is not found");
                                }
                                log.error("Retry...");
                            }
                            if (midiPresetIdentity != null) {
                                log.info("Bank  name : " + midiPresetIdentity.bankName());
                                log.info("Patch name : " + midiPresetIdentity.name());
                                log.info("Category   : " + midiPresetIdentity.category());
                                log.info("Preset     : " + midiPreset.getConfig());
                                log.info("");
                                midiPreset.setId(midiPresetIdentity);
                                midiPresetConsumer.onNewMidiPreset(device, midiPreset);
                            }
                            previousPatchIdentity = midiPresetIdentity;
                        }
                    }
                }
            }
            in.stopListening();
        } catch (MidiUnavailableException e) {
            throw new MidiError(e);
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                throw new MidiError(e);
            }
            try {
                in.close();
            } catch (IOException e) {
                throw new MidiError(e);
            }
        }
    }

    private void send(MidiRequestSequence sequence, MidiOutDevice out) {
        try {
            for (var request : sequence.getMidiRequests()) {
                List<CustomMidiEvent> requestInstances = SysExBuilder.parse(request.getValue());
                for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                    var customMidiEvent = requestInstances.get(requestInstanceIndex);
                    log.info("Send {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                    out.send(customMidiEvent);
                }
            }
        } catch (InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private void onResponse(MidiInDevice midiInDevice, CustomMidiEvent customMidiEvent) {
        try {
            currentSysex.write(customMidiEvent.getMessage()
                    .getMessage());
            /*log.info("Receive %d bytes, current total: 0x%X".formatted(customMidiEvent.getMessage()
                    .getMessage().length, currentSysex.size()));*/
            if (expectedResponseSize > 0 && currentSysex.size() == expectedResponseSize) {
                currentResponse.set(new CustomMidiEvent(new SysexMessage(currentSysex.toByteArray(), currentSysex.size()), 0));
            } else if (expectedResponseSize == 0) {
                currentResponse.set(customMidiEvent);
            }
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private MidiRequestSequence forgeRequestSequence(MidiDeviceLibrary library, MidiDeviceDefinition device, String command) {
        if (command == null) {
            return null;
        }
        var sequences = CommandCall.parse(device.getDefinitionFile(), command)
                .stream()
                .map(commandCall -> {
                    CommandMacro macro = device.getMacro(commandCall);
                    return library.forgeMidiRequestSequence(device.getDefinitionFile(), device.getDeviceName(), macro, commandCall);
                })
                .toList();
        Integer sum = sequences.stream()
                .map(s -> s.getTotalSize())
                .filter(i -> i != null)
                .mapToInt(Integer::intValue)
                .sum();
        return new MidiRequestSequence(sum, sequences.stream()
                .flatMap(s -> s.getMidiRequests()
                        .stream())
                .toList());
    }

    void resetCurrentResponse() {
        currentResponse.set(null);
        currentSysex.reset();
    }

    CustomMidiEvent waitResponse() {
        wait(() -> currentResponse.get() != null);
        return currentResponse.get();
    }

    private MidiPresetIdentity getStandardPreset(MidiDeviceMode mode, String currentBankName, int bankMSB, int bankLSB, int program, MidiRequestSequence queryNameRequestSequence, MidiOutDevice out) throws InvalidMidiDataException {
        var response = requestFields(mode, bankMSB, bankLSB, program, queryNameRequestSequence, out);
        if (response.getPatchName() != null) {
            return new MidiPresetIdentity(mode.getName(), currentBankName, response.getPatchName(), response.getCategory());
        } else {
            return null;
        }
    }

    private MidiPresetIdentity getSoundCanvasPreset(MidiDeviceDefinition device, MidiDeviceMode mode, int program, MidiPreset midiPreset) {
        int bankMSB = midiPreset.getBankMSB();
        MidiDeviceBank bank = device.getBankByMSB("" + bankMSB)
                .orElseThrow(() -> new MidiConfigError("Bank MSB %d not declared in presetBank section of decide '%'".formatted(bankMSB, device.getDeviceName())));
        String presetName = getSoundCanvasPresetName(bankMSB, midiPreset.getBankLSB(), program).orElse("Unknown");
        MidiPresetCategory category = getSoundCanvasCategory(device, mode, program);
        return new MidiPresetIdentity(mode.getName(), bank.getName(), presetName, category.name());
    }

    private ExtractedFields requestFields(MidiDeviceMode mode, int bankMSB, int bankLSB, int program, MidiRequestSequence sequence, MidiOutDevice out) throws InvalidMidiDataException {
        ExtractedFields response = null;
        expectedResponseSize = sequence.getTotalSize();
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = SysExBuilder.parse(request.getValue()
                    .replace("program", "%02X".formatted(program))
                    .replace("bankMSB", "%02X".formatted(bankMSB))
                    .replace("bankLSB", "%02X".formatted(bankLSB))
            );
            for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                var customMidiEvent = requestInstances.get(requestInstanceIndex);
                CustomMidiEvent midiEvent = null;
                for (int retry = 0; retry < 4; retry++) {
                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                    response = new ExtractedFields();
                    resetCurrentResponse();
                    out.send(customMidiEvent);
                    try {
                        midiEvent = waitResponse();
                        int receivedSize = midiEvent.getMessage()
                                .getLength();
                        log.info("Received " + receivedSize + " bytes ($%X)".formatted(receivedSize));
                        dumpResonse(midiEvent);
                        if (sequence.getTotalSize() == 0 || receivedSize == sequence.getTotalSize()) {
                            break;
                        } else {
                            log.error("Wrong size received: " + receivedSize + " bytes instead of " + sequence.getTotalSize());
                            break;
                        }
                    } catch (MidiDeviceTimeout timeout) {
                        log.warn("No response after %d seconds, Retry...".formatted(timeout.getTimeoutInSec()));
                    }
                }
                // extract fields from the response
                if (request.getMapper() != null && midiEvent != null && midiEvent.getMessage() != null && response != null) {
                    request.getMapper()
                            .extract(mode, response, midiEvent);
                    request.getMapper()
                            .dumpFields(response);
                }
            }
        }

        return response;
    }

    private static void dumpResonse(CustomMidiEvent midiEvent) {
        try {
            Files.write(Path.of("response.syx"), midiEvent.getMessage()
                    .getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MidiPresetCategory getSoundCanvasCategory(MidiDeviceDefinition device, MidiDeviceMode mode, int program) {
        return device.getCategory(mode, program / 8);
    }

    private Optional<String> getSoundCanvasPresetName(int bankMSB, int bankLSB, int program) {
        String prefix = "%d-%d-%d ".formatted(bankMSB, bankLSB, program);
        URL resource = this.getClass()
                .getClassLoader()
                .getResource("sc/SoundCanvasPatches.txt");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .toList()
                    .stream()
                    .filter(l -> l.startsWith(prefix))
                    .map(this::parseEntry)
                    .flatMap(Optional::stream)
                    .findFirst();
        } catch (Exception e) {
            throw new MidiConfigError("Erreur lors de la lecture de l'InputStream", e); // Gestion des exceptions améliorée
        }
    }

    private Optional<String> parseEntry(String soundCanvasPresetDefinition) {
        var m = SOUND_CANVAS_PRESET_DEFINITION_REGEXP.matcher(soundCanvasPresetDefinition);
        if (m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    private void wait(Supplier<Boolean> predicate) {
        int size = currentSysex.size();
        long start = System.currentTimeMillis();
        do {
            wait(null, 50);
            long now = System.currentTimeMillis();
            if (currentSysex.size() == size && now - start > 1000 * 4) {
                int timeoutInSec = (int) (now - start) / 1000;
                throw new MidiDeviceTimeout("No response from the device. SysEx request seems inappropriate", timeoutInSec);
            } else if (currentSysex.size() != size) {
                size = currentSysex.size();
            }
        }
        while (!predicate.get());
    }

    private static void wait(String msg, int timeMs) {
        try {
            if (msg != null) {
                log.info("{} : {} ms...", msg, timeMs);
            }
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

