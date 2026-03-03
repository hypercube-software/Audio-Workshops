package com.hypercube.workshop.midiworkshop.api.presets.generic;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
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
    private final MidiPortsManager midiPortsManager;
    private final Pattern SOUND_CANVAS_PRESET_DEFINITION_REGEXP = Pattern.compile("\\d+-\\d+-\\d+\\s(.+)");
    private final AtomicReference<CustomMidiEvent> currentResponse = new AtomicReference<>();
    private final List<String> xgPresets;
    private final List<String> scPresets;
    private final ByteArrayOutputStream currentSysEx = new ByteArrayOutputStream();
    private int expectedResponseSize = 0;

    public MidiPresetCrawler(MidiDeviceLibrary library, MidiDeviceRequester midiDeviceRequester, MidiPortsManager midiPortsManager) {
        xgPresets = loadXGPresets();
        scPresets = loadSoundCanvasPreset();
        this.library = library;
        this.midiDeviceRequester = midiDeviceRequester;
        this.midiPortsManager = midiPortsManager;
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

    public void crawlAllPatches(CrawlingDomain crawlingDomain, MidiPresetConsumer midiPresetConsumer) {
        MidiDeviceDefinition device = library.getDevice(crawlingDomain.device())
                .orElseThrow(() -> new MidiConfigError("Device not declared in the library: " + crawlingDomain.device()));

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

                int nbPresetToQuery = countPresets(device, crawlingDomain);
                int currentPresetCount = 1;
                try {
                    MidiPresetIdentity previousPatchIdentity = null;
                    in.open();
                    in.addSysExListener(this::onResponse);
                    out.open();
                    var inputModes = device.getDeviceModes()
                            .values()
                            .stream()
                            .filter(crawlingDomain::matches)
                            .toList();
                    for (var mode : inputModes) {
                        changeMode(mode, device, out);
                        MidiRequestSequence modeRequestSequence = forgeRequestSequence(device, mode.getQueryName());
                        MidiRequestSequence modePreRequestSequence = mode.getPreQueryName() != null ? forgeRequestSequence(device, mode.getPreQueryName()) : null;
                        MidiRequestSequence modePostRequestSequence = mode.getPostQueryName() != null ? forgeRequestSequence(device, mode.getPostQueryName()) : null;
                        var inputBanks = mode.getModeBanks()
                                .stream()
                                .filter(crawlingDomain::matches)
                                .toList();
                        for (var bank : inputBanks) {
                            if (bank.getPresetDomain() == null) {
                                continue;
                            }
                            MidiRequestSequence bankRequestSequence = bank.getQueryName() != null ? forgeRequestSequence(device, bank.getQueryName()) : modeRequestSequence;
                            MidiRequestSequence bankPreRequestSequence = bank.getPreQueryName() != null ? forgeRequestSequence(device, bank.getPreQueryName()) : modePreRequestSequence;
                            MidiRequestSequence bankPostRequestSequence = bank.getPostQueryName() != null ? forgeRequestSequence(device, bank.getPostQueryName()) : modePostRequestSequence;
                            if (bankRequestSequence == null) {
                                log.error("Bank '{}' for device '{}' has no queryName defined", bank.getName(), device.getDeviceName());
                                continue;
                            }
                            for (var range : bank.getPresetDomain()
                                    .getRanges()) {
                                for (int program : IntStream.rangeClosed(range.getFrom(), range.getTo())
                                        .toArray()) {
                                    String currentBankName = bank.getName();
                                    MidiPreset midiPreset = MidiPresetBuilder.parse(device, mode, bank, program);
                                    selectPatch(midiPreset, out);
                                    // let the time the edit buffer is completely set before querying it
                                    wait("Wait patch change", device.getPresetLoadTimeMs());
                                    MidiPresetIdentity midiPresetIdentity = null;
                                    MidiPresetNaming presetNaming = mode.getPresetNaming() != null ? mode.getPresetNaming() : device.getPresetNaming();
                                    for (int retry = 0; retry < 2; retry++) {
                                        midiPresetIdentity = switch (presetNaming) {
                                            case STANDARD ->
                                                    getStandardPreset(device, mode, currentBankName, midiPreset,
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
                                                log.warn("Something may be wrong, the patch name is the same than the previous one, try increase 'presetLoadTime'");
                                            }
                                            break;
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
                                        midiPresetConsumer.onNewMidiPreset(device, midiPreset, currentPresetCount, nbPresetToQuery);
                                        currentPresetCount++;
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

    private void selectPatch(MidiPreset midiPreset, MidiOutDevice out) {
        log.info("Select Bank {}' Program '{}' in mode '{}'", midiPreset.getId()
                .bankName(), midiPreset.getId()
                .name(), midiPreset.getId()
                .deviceMode());
        for (var command : midiPreset.getCommands()) {
            CustomMidiEvent cm = new CustomMidiEvent(command);
            log.info("    {}", cm.getHexValuesSpaced());
            out.send(cm);
        }
    }

    private int countPresets(MidiDeviceDefinition device, CrawlingDomain crawlingDomain) {
        int presetCount = 0;
        List<MidiDeviceMode> inputModes = device.getDeviceModes()
                .values()
                .stream()
                .filter(crawlingDomain::matches)
                .toList();
        for (var mode : inputModes) {
            List<MidiDeviceBank> inputBanks = mode.getBanks()
                    .values()
                    .stream()
                    .filter(crawlingDomain::matches)
                    .toList();
            for (var bank : inputBanks) {
                if (bank.getPresetDomain() == null) {
                    continue;
                }
                for (var range : bank.getPresetDomain()
                        .getRanges()) {
                    for (int program : IntStream.rangeClosed(range.getFrom(), range.getTo())
                            .toArray()) {
                        presetCount++;
                    }
                }
            }
        }
        return presetCount;
    }

    private void changeMode(MidiDeviceMode mode, MidiDeviceDefinition device, MidiOutDevice out) {
        log.info("Set mode " + mode.getName());
        // no command mean the device switch automatically to the right mode (Like Yamaha TG-500)
        if (mode.getCommand() != null) {
            MidiRequestSequence setModeRequestSequence = forgeRequestSequence(device, mode.getCommand());
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
            throw new MidiConfigError("Unable to open " + resource, e);
        }
    }

    private void populateDrumKitMap(MidiPresetNaming presetNaming, MidiPreset midiPreset) {
        String prefix = "%d-%d-%d".formatted(midiPreset.getBankMSB(), midiPreset.getBankLSB(), midiPreset.getLastProgram());
        if (presetNaming == MidiPresetNaming.YAMAHA_XG) {
            boolean inPreset = false;
            for (String preset : xgPresets) {
                if (preset.startsWith(" ") && inPreset) {
                    Pattern drumMapEntry = Pattern.compile("\\s+([0-9]+)\\s(.+)");
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
            currentSysEx.write(customMidiEvent.getMessage()
                    .getMessage());
            /*log.info("Receive %d bytes, current total: 0x%X".formatted(customMidiEvent.getMessage()
                    .getMessage().length, currentSysex.size()));*/
            if (expectedResponseSize > 0 && currentSysEx.size() == expectedResponseSize) {
                currentResponse.set(new CustomMidiEvent(new SysexMessage(currentSysEx.toByteArray(), currentSysEx.size())));
            } else if (expectedResponseSize == 0) {
                currentResponse.set(customMidiEvent);
            }
        } catch (IOException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    private MidiRequestSequence forgeRequestSequence(MidiDeviceDefinition device, String command) {
        if (command == null) {
            return null;
        }
        var sequences = CommandCall.parse(device.getDefinitionFile(), device, command)
                .stream()
                .map(commandCall -> midiDeviceRequester.forgeMidiRequestSequence(device, commandCall))
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

    private MidiPresetIdentity getStandardPreset(MidiDeviceDefinition device, MidiDeviceMode mode, String currentBankName, MidiPreset midiPreset,
                                                 MidiRequestSequence preSequence,
                                                 MidiRequestSequence sequence,
                                                 MidiRequestSequence postSequence,
                                                 MidiOutDevice out) throws InvalidMidiDataException {
        final var response = requestFields(device, mode, midiPreset, preSequence, sequence, postSequence, out);
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

    private ExtractedFields requestFields(MidiDeviceDefinition device, MidiDeviceMode mode, MidiPreset midiPreset,
                                          MidiRequestSequence preSequence,
                                          MidiRequestSequence sequence,
                                          MidiRequestSequence postSequence,
                                          MidiOutDevice out) throws InvalidMidiDataException {
        sendPreSequence(device, preSequence, out);
        ExtractedFields response = null;
        expectedResponseSize = sequence.getTotalSize();
        if (expectedResponseSize == 0) {
            log.warn("Response size is unknown, this will considerably slow down the extraction... Try to put response sizes in your macros");
        }
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = MidiEventBuilder.parse(request.getValue()
                    .replace("program", "%02X".formatted(midiPreset.getBankPrg()))
                    .replace("bankMSB", "%02X".formatted(midiPreset.getBankMSB()))
                    .replace("bankLSB", "%02X".formatted(midiPreset.getBankLSB()))
            );
            for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                final var customMidiEvent = requestInstances.get(requestInstanceIndex);
                CustomMidiEvent midiResponse = null;
                for (int retry = 0; retry < 4; retry++) {
                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                    response = new ExtractedFields();
                    resetCurrentResponse();
                    out.send(customMidiEvent);
                    try {
                        midiResponse = waitResponse();
                        int receivedSize = midiResponse.getMessage()
                                .getLength();
                        log.info("Received " + receivedSize + " bytes ($%X)".formatted(receivedSize));
                        dumpResponse(midiResponse);
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
                if (request.getMapper() != null && midiResponse != null && midiResponse.getMessage() != null) {
                    request.getMapper()
                            .extract(mode, response, midiResponse);
                    request.getMapper()
                            .dumpFields(response);
                }
            }
        }
        sendPostSequence(device, postSequence, out);

        return response;
    }

    private void sendPreSequence(MidiDeviceDefinition device, MidiRequestSequence preSequence, MidiOutDevice out) {
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

    private void sendPostSequence(MidiDeviceDefinition device, MidiRequestSequence postSequence, MidiOutDevice out) {
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
        int size = currentSysEx.size();
        long start = System.currentTimeMillis();
        do {
            wait(null, 50);
            long now = System.currentTimeMillis();
            if (currentSysEx.size() == size && now - start > 1000 * 4) {
                int timeoutInSec = (int) (now - start) / 1000;
                throw new MidiDeviceTimeout("No response from the device. SysEx request seems inappropriate", timeoutInSec);
            } else if (currentSysEx.size() != size) {
                size = currentSysEx.size();
            }
        }
        while (!predicate.get());
    }

    void resetCurrentResponse() {
        currentResponse.set(null);
        currentSysEx.reset();
    }

    CustomMidiEvent waitResponse() {
        wait(() -> currentResponse.get() != null);
        return currentResponse.get();
    }
}
