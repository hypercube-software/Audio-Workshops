package com.hypercube.workshop.midiworkshop.api.presets.generic;

import com.hypercube.workshop.midiworkshop.MidiWorkshopApplication;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiDeviceTimeout;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.*;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * The MidiPresetCrawler is able to query a Midi device to retrieve various fields for all patches
 * <p>The whole process can take time but at the end, you can generate the list of patches automatically</p>
 * <ul>
 *     <li>The Midi device has to provide the required fields through sysex responses: patch name, patch category</li>
 *     <li>The extraction is performed by {@link MidiResponseMapper}</li>
 *     <li>Categories don't have to be strings, a simple byte will point to {@link MidiDeviceDefinition#getCategories()}</li>
 * </ul>
 * The caller will receive all the {@link MidiPreset} through the functional interface {@link MidiPresetConsumer}
 * <ul>
 *     <li>{@link MidiPreset#getId()} provides the {@link MidiPresetIdentity} of a patch: device,mode,bank,name and category</li>
 *     <li>{@link MidiPreset#getCommands()} provides the list of {@link MidiMessage} to select the preset on the device</li>
 * </ul>
 * When the device does not support patch names in Sysex, we can rely on hardcoded lists.
 * <ul>
 *     <li>This is the case for sound canvas devices. We use {@link MidiPresetNaming#SOUND_CANVAS} in this case</li>
 *     <li>Other devices are not supported</li>
 * </ul>
 */
@Slf4j
@Service
public class MidiPresetCrawler {
    private final MidiDeviceLibrary library;
    private final MidiDeviceRequester midiDeviceRequester;
    private final Pattern SOUND_CANVAS_PRESET_DEFINITION_REGEXP = Pattern.compile("\\d+-\\d+-\\d+\\s(.+)");
    private final AtomicReference<CustomMidiEvent> currentResponse = new AtomicReference<>();
    private final List<String> xgPresets;
    private final List<String> scPresets;
    private final ByteArrayOutputStream currentSysex = new ByteArrayOutputStream();
    private int expectedResponseSize = 0;

    public MidiPresetCrawler(MidiDeviceLibrary library, MidiDeviceRequester midiDeviceRequester) {
        xgPresets = loadXGPresets();
        scPresets = loadSoundCanvasPreset();
        this.library = library;
        this.midiDeviceRequester = midiDeviceRequester;
    }

    private static void dumpResponse(CustomMidiEvent midiEvent) {
        try {
            Files.write(Path.of("response.syx"), midiEvent.getMessage()
                    .getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void crawlAllPatches(String deviceName, MidiPresetConsumer midiPresetConsumer) {
        library.load(ConfigHelper.getApplicationFolder(MidiWorkshopApplication.class));
        MidiDeviceDefinition device = library.getDevice(deviceName)
                .orElseThrow(() -> new MidiConfigError("Device not declared in the library: " + deviceName));

        MidiPortsManager midiPortsManager = new MidiPortsManager();
        midiPortsManager.collectDevices();
        String outputMidiDevice = device.getOutputMidiDevice();
        try (MidiOutDevice out = midiPortsManager.getOutput(outputMidiDevice)
                .orElse(null)) {
            String inputMidiDevice = device.getInputMidiDevice();
            try (MidiInDevice in = midiPortsManager.getInput(inputMidiDevice)
                    .orElse(null)) {
                if (out == null) {
                    throw new MidiConfigError("MIDI OUT Device not found: '%s".formatted(outputMidiDevice));
                }
                if (in == null) {
                    throw new MidiConfigError("MIDI IN Device not found: '%s".formatted(inputMidiDevice));
                }

                int nbPresetToQuery = countPresets(device);
                int currenPresetCount = 1;
                try {
                    MidiPresetIdentity previousPatchIdentity = null;
                    in.open();
                    in.addSysExListener(this::onResponse);
                    out.open();
                    for (var mode : device.getDeviceModes()
                            .values()) {
                        changeMode(mode, library, device, out);
                        MidiRequestSequence modeRequestSequence = forgeRequestSequence(library, device, mode.getQueryName());
                        MidiRequestSequence modePreRequestSequence = mode.getPreQueryName() != null ? forgeRequestSequence(library, device, mode.getPreQueryName()) : null;
                        MidiRequestSequence modePostRequestSequence = mode.getPostQueryName() != null ? forgeRequestSequence(library, device, mode.getPostQueryName()) : null;
                        for (var bank : mode.getBanks()
                                .values()) {
                            if (bank.getPresetDomain() == null) {
                                continue;
                            }
                            MidiRequestSequence bankRequestSequence = bank.getQueryName() != null ? forgeRequestSequence(library, device, bank.getQueryName()) : modeRequestSequence;
                            MidiRequestSequence bankPreRequestSequence = bank.getPreQueryName() != null ? forgeRequestSequence(library, device, bank.getPreQueryName()) : modePreRequestSequence;
                            MidiRequestSequence bankPostRequestSequence = bank.getPostQueryName() != null ? forgeRequestSequence(library, device, bank.getPostQueryName()) : modePostRequestSequence;
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
                                        CustomMidiEvent cm = new CustomMidiEvent(command);
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
                                    //wait("Wait patch change", device.getPresetLoadTimeMs());
                                    MidiPresetIdentity midiPresetIdentity = null;
                                    MidiPresetNaming presetNaming = mode.getPresetNaming() != null ? mode.getPresetNaming() : device.getPresetNaming();
                                    for (int retry = 0; retry < 2; retry++) {
                                        midiPresetIdentity = switch (presetNaming) {
                                            case STANDARD ->
                                                    getStandardPreset(device, mode, currentBankName, bankMSB, bankLSB, program,
                                                            bankPreRequestSequence,
                                                            bankRequestSequence,
                                                            bankPostRequestSequence, out);
                                            case SOUND_CANVAS ->
                                                    getPredefinedPreset(scPresets, device, mode, program, midiPreset);
                                            case YAMAHA_XG ->
                                                    getPredefinedPreset(xgPresets, device, mode, program, midiPreset);
                                        };
                                        if (midiPresetIdentity != null) {
                                            if (previousPatchIdentity != null && previousPatchIdentity.name()
                                                    .equals(midiPresetIdentity.name())) {
                                                log.error("Something wrong, the patch name is the same than the previous one");
                                            } else {
                                                break;
                                            }
                                        } else {
                                            log.error("Something wrong, the patch name is not found");
                                        }
                                        log.error("Retry...");
                                    }
                                    if (midiPresetIdentity != null) {
                                        if (presetNaming != MidiPresetNaming.STANDARD) {
                                            populateDrumKitMap(presetNaming, midiPreset);
                                        }
                                        log.info("Bank  name  : " + midiPresetIdentity.bankName());
                                        log.info("Patch name  : " + midiPresetIdentity.name());
                                        log.info("Category    : " + midiPresetIdentity.category());
                                        log.info("Preset Cmd  : " + midiPreset.getCommand());
                                        log.info("Program Chg : " + program);
                                        if (!midiPreset.getDrumKitNotes()
                                                .isEmpty()) {
                                            log.info("DrumMap    : " + midiPreset.getDrumKitNotes()
                                                    .size() + " notes");
                                        }
                                        log.info("");
                                        midiPreset.setId(midiPresetIdentity);
                                        midiPresetConsumer.onNewMidiPreset(device, midiPreset, currenPresetCount, nbPresetToQuery);
                                        currenPresetCount++;
                                    }
                                    previousPatchIdentity = midiPresetIdentity;
                                }
                            }
                        }
                    }
                } catch (InvalidMidiDataException e) {
                    throw new MidiError(e);
                }
            }
        }
    }

    private int countPresets(MidiDeviceDefinition device) {
        int presetcount = 0;
        for (var mode : device.getDeviceModes()
                .values()) {
            for (var bank : mode.getBanks()
                    .values()) {
                if (bank.getPresetDomain() == null) {
                    continue;
                }
                for (var range : bank.getPresetDomain()
                        .getRanges()) {
                    for (int program : IntStream.rangeClosed(range.getFrom(), range.getTo())
                            .toArray()) {
                        presetcount++;
                    }
                }
            }
        }
        return presetcount;
    }

    private void changeMode(MidiDeviceMode mode, MidiDeviceLibrary library, MidiDeviceDefinition device, MidiOutDevice out) {
        log.info("Set mode " + mode.getName());
        // no command mean the device switch automatically to the right mode (Like Yamaha TG-500)
        if (mode.getCommand() != null) {
            MidiRequestSequence setModeRequestSequence = forgeRequestSequence(library, device, mode.getCommand());
            send(setModeRequestSequence, out);
            wait("Wait mode change", device.getModeLoadTimeMs());
        }
    }

    private List<String> loadSoundCanvasPreset() {
        return loadAllText("sc/SoundCanvasPatches.txt");
    }

    private List<String> loadXGPresets() {
        return loadAllText("xg/XGPatches.txt");
    }

    private List<String> loadAllText(String resourcePath) {
        URL resource = this.getClass()
                .getClassLoader()
                .getResource(resourcePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .toList();
        } catch (Exception e) {
            throw new MidiConfigError("Unable to open " + resource, e); // Gestion des exceptions améliorée
        }
    }

    private void populateDrumKitMap(MidiPresetNaming presetNaming, MidiPreset midiPreset) {
        String prefix = "%d-%d-%d".formatted(midiPreset.getBankMSB(), midiPreset.getBankLSB(), midiPreset.getLastProgram());
        if (presetNaming == MidiPresetNaming.YAMAHA_XG) {
            boolean inPreset = false;
            for (int i = 0; i < xgPresets.size(); i++) {
                String preset = xgPresets.get(i);
                if (preset.startsWith(" ") && inPreset) {
                    Pattern drumMapEntry = Pattern.compile("\s+([0-9]+)\s(.+)");
                    Matcher m = drumMapEntry.matcher(preset);
                    if (m.matches()) {
                        int note = Integer.parseInt(m.group(1));
                        String title = m.group(2);
                        midiPreset.getDrumKitNotes()
                                .add(new DrumKitNote(title, note));
                    }
                }
                if (!preset.startsWith(" ") && preset.startsWith(prefix)) {
                    inPreset = true;
                } else if (!preset.startsWith(" ")) {
                    inPreset = false;
                }
            }
        }
    }

    private void send(MidiRequestSequence sequence, MidiOutDevice out) {
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = MidiEventBuilder.parse(request.getValue());
            for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                var customMidiEvent = requestInstances.get(requestInstanceIndex);
                log.info("Send {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                out.send(customMidiEvent);
            }
        }
    }

    private void onResponse(MidiInDevice midiInDevice, CustomMidiEvent customMidiEvent) {
        try {
            currentSysex.write(customMidiEvent.getMessage()
                    .getMessage());
            /*log.info("Receive %d bytes, current total: 0x%X".formatted(customMidiEvent.getMessage()
                    .getMessage().length, currentSysex.size()));*/
            if (expectedResponseSize > 0 && currentSysex.size() == expectedResponseSize) {
                currentResponse.set(new CustomMidiEvent(new SysexMessage(currentSysex.toByteArray(), currentSysex.size())));
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
                    return midiDeviceRequester.forgeMidiRequestSequence(device, macro, commandCall);
                })
                .toList();
        Integer sum = sequences.stream()
                .map(MidiRequestSequence::getTotalSize)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        return new MidiRequestSequence(sum, sequences.stream()
                .flatMap(s -> s.getMidiRequests()
                        .stream())
                .toList());
    }

    private MidiPresetIdentity getStandardPreset(MidiDeviceDefinition device, MidiDeviceMode mode, String currentBankName, int bankMSB, int bankLSB,
                                                 int program,
                                                 MidiRequestSequence preCommand,
                                                 MidiRequestSequence queryNameRequestSequence,
                                                 MidiRequestSequence postCommand,
                                                 MidiOutDevice out) throws InvalidMidiDataException {
        var response = requestFields(device, mode, bankMSB, bankLSB, program, preCommand, queryNameRequestSequence, postCommand, out);
        if (response.getPatchName() != null) {
            return new MidiPresetIdentity(mode.getName(), currentBankName, response.getPatchName(), response.getCategory());
        } else {
            return null;
        }
    }

    private MidiPresetIdentity getPredefinedPreset(List<String> presets, MidiDeviceDefinition device, MidiDeviceMode mode, int program, MidiPreset midiPreset) {
        String bankCommand = midiPreset.getBankCommand();
        MidiDeviceBank bank = device.getBankByCommand(bankCommand)
                .orElseThrow(() -> new MidiConfigError("Bank command %s not declared in presetBank section of device '%s'".formatted(bankCommand, device.getDeviceName())));
        String presetName = getPredefinedPresetName(presets, midiPreset.getBankMSB(), midiPreset.getBankLSB(), program).orElse("Unknown");
        MidiPresetCategory category = getCategoryFromProgram(device, mode, bank, program);
        return new MidiPresetIdentity(mode.getName(), bank.getName(), presetName, category.name());
    }

    private ExtractedFields requestFields(MidiDeviceDefinition device, MidiDeviceMode mode, int bankMSB, int bankLSB, int program,
                                          MidiRequestSequence preSequence,
                                          MidiRequestSequence sequence,
                                          MidiRequestSequence postSequence,
                                          MidiOutDevice out) throws InvalidMidiDataException {
        sendPreSequence(device, preSequence, out);
        ExtractedFields response = null;
        expectedResponseSize = sequence.getTotalSize();
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = MidiEventBuilder.parse(request.getValue()
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
                        dumpResponse(midiEvent);
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
        sendPostSequence(device, postSequence, out);

        return response;
    }

    private void sendPreSequence(MidiDeviceDefinition device, MidiRequestSequence preSequence, MidiOutDevice out) throws InvalidMidiDataException {
        if (preSequence != null) {
            for (var request : preSequence.getMidiRequests()) {
                MidiEventBuilder.parse(request.getValue())
                        .forEach(evt -> {
                            log.info("Pre request command \"{}\": {}", request.getName(), evt.getHexValuesSpaced());
                            resetCurrentResponse();
                            out.send(evt);
                            if (request.getResponseSize() != null) {
                                var resp = waitResponse();
                                int receivedSize = resp.getMessage()
                                        .getLength();
                                log.info("Received " + receivedSize + " bytes ($%X)".formatted(receivedSize));
                            }
                        });
            }
            wait("Wait pre sequence done", device.getModeLoadTimeMs());
        }
    }

    private void sendPostSequence(MidiDeviceDefinition device, MidiRequestSequence postSequence, MidiOutDevice out) throws InvalidMidiDataException {
        if (postSequence != null) {
            for (var request : postSequence.getMidiRequests()) {
                MidiEventBuilder.parse(request.getValue())
                        .forEach(evt -> {
                            log.info("Post request command \"{}\": {}", request.getName(), evt.getHexValuesSpaced());
                            out.send(evt);
                            if (request.getResponseSize() != null) {
                                var resp = waitResponse();
                                int receivedSize = resp.getMessage()
                                        .getLength();
                                log.info("Received " + receivedSize + " bytes ($%X)".formatted(receivedSize));
                            }
                        });
            }
            wait("Wait post sequence done", device.getModeLoadTimeMs());
        }
    }

    private MidiPresetCategory getCategoryFromProgram(MidiDeviceDefinition device, MidiDeviceMode mode, MidiDeviceBank bank, int program) {
        int categoryIndex = bank.getCategory() != null ? bank.getCategory() : program / 8;
        return device.getCategory(mode, categoryIndex);
    }

    private Optional<String> getPredefinedPresetName(List<String> presets, int bankMSB, int bankLSB, int program) {
        String prefix = "%d-%d-%d ".formatted(bankMSB, bankLSB, program);
        return presets
                .stream()
                .filter(l -> l.startsWith(prefix))
                .map(this::parseEntry)
                .flatMap(Optional::stream)
                .findFirst();
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

    void resetCurrentResponse() {
        currentResponse.set(null);
        currentSysex.reset();
    }

    CustomMidiEvent waitResponse() {
        wait(() -> currentResponse.get() != null);
        return currentResponse.get();
    }
}
