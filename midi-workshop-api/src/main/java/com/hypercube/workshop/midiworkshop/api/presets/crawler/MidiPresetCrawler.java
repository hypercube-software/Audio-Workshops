package com.hypercube.workshop.midiworkshop.api.presets.crawler;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.errors.MidiDeviceTimeout;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.presets.*;
import com.hypercube.workshop.midiworkshop.api.presets.standard.GSPresetsContainer;
import com.hypercube.workshop.midiworkshop.api.presets.standard.StandardPresetsContainer;
import com.hypercube.workshop.midiworkshop.api.presets.standard.XGPresetsContainer;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardPreset;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.MidiDeviceRequester;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.ExtractedFields;
import com.hypercube.workshop.midiworkshop.api.sysex.library.io.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import com.hypercube.workshop.midiworkshop.api.thread.CancelNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.SysexMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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
    private final AtomicReference<CustomMidiEvent> currentResponse = new AtomicReference<>();
    private final XGPresetsContainer xgPresetsContainer;
    private final GSPresetsContainer gsPresetsContainer;
    private final ByteArrayOutputStream currentSysEx = new ByteArrayOutputStream();
    private int expectedResponseSize = 0;

    public MidiPresetCrawler(MidiDeviceLibrary library, MidiDeviceRequester midiDeviceRequester, MidiPortsManager midiPortsManager) {
        this.xgPresetsContainer = new XGPresetsContainer();
        this.gsPresetsContainer = new GSPresetsContainer();
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

    public void crawlAllPatches(CrawlingDomain crawlingDomain, MidiPresetConsumer midiPresetConsumer, CancelNotifier cancelNotifier) {
        MidiDeviceDefinition device = library.getDevice(crawlingDomain.device())
                .orElseThrow(() -> new MidiConfigError("Device not declared in the library: " + crawlingDomain.device()));

        String outputMidiDevice = device.getOutputMidiDevice();
        try (MidiOutPort out = midiPortsManager.getOutput(outputMidiDevice)
                .orElse(null)) {
            String inputMidiDevice = device.getInputMidiDevice();
            try (MidiInPort in = midiPortsManager.getInput(inputMidiDevice)
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
                                        checkIfShouldStop(cancelNotifier);
                                        midiPresetIdentity = switch (presetNaming) {
                                            case STANDARD ->
                                                    getStandardPreset(cancelNotifier, device, mode, currentBankName, midiPreset,
                                                            bankPreRequestSequence,
                                                            bankRequestSequence,
                                                            bankPostRequestSequence, out);
                                            case SOUND_CANVAS ->
                                                    getPredefinedPreset(gsPresetsContainer, device, mode, program, midiPreset);
                                            case YAMAHA_XG ->
                                                    getPredefinedPreset(xgPresetsContainer, device, mode, program, midiPreset);
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
                                        log.info("Bank  name  : {}", midiPresetIdentity.bankName());
                                        log.info("Patch name  : {}", midiPresetIdentity.name());
                                        log.info("Category    : {}", midiPresetIdentity.category());
                                        log.info("Preset Cmd  : {}", midiPreset.getCommand());
                                        log.info("Program Chg : {}", program);
                                        if (!midiPreset.getDrumKitNotes()
                                                .isEmpty()) {
                                            log.info("DrumMap    : {} notes", midiPreset.getDrumKitNotes()
                                                    .size());
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
                } catch (CancellationException e) {
                    log.warn("Operation cancelled by user");
                } catch (InvalidMidiDataException e) {
                    throw new MidiError(e);
                }
            }
        }
    }

    private void checkIfShouldStop(CancelNotifier cancelNotifier) {
        Optional.ofNullable(cancelNotifier)
                .ifPresent(CancelNotifier::checkIfShouldStop);
    }

    private void selectPatch(MidiPreset midiPreset, MidiOutPort out) {
        String prg = Optional.ofNullable(midiPreset.getIdentifiers())
                .map(PresetIdentifiers::getPrg)
                .map("%d"::formatted)
                .orElse("NOT SET");
        log.info("Select Bank '{}' Program '{}' in mode '{}'", midiPreset.getId()
                .bankName(), prg, midiPreset.getId()
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

    private void changeMode(MidiDeviceMode mode, MidiDeviceDefinition device, MidiOutPort out) {
        log.info("Set mode {}", mode.getName());
        if (mode.getCommand() != null) {
            MidiRequestSequence setModeRequestSequence = forgeRequestSequence(device, mode.getCommand());
            send(setModeRequestSequence, out);
            wait("Wait mode change", device.getModeLoadTimeMs());
        }
    }

    private void populateDrumKitMap(MidiPresetNaming presetNaming, MidiPreset midiPreset) {
        if (presetNaming == MidiPresetNaming.YAMAHA_XG) {
            xgPresetsContainer.lookup(midiPreset)
                    .ifPresent(stdPreset -> {
                        midiPreset.getDrumKitNotes()
                                .addAll(stdPreset.drumMap());
                    });
        }
    }

    private void send(MidiRequestSequence sequence, MidiOutPort out) {
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = MidiEventBuilder.parse(request.getValue());
            for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                var customMidiEvent = requestInstances.get(requestInstanceIndex);
                log.info("Send {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                out.send(customMidiEvent);
            }
        }
    }

    private void onResponse(MidiInPort hardwareMidiInPort, CustomMidiEvent customMidiEvent) {
        try {
            if (customMidiEvent.getMessage() instanceof SysexMessage sysexMessage) {
                currentSysEx.write(sysexMessage.getMessage());

                if (expectedResponseSize > 0 && currentSysEx.size() == expectedResponseSize) {
                    currentResponse.set(new CustomMidiEvent(new SysexMessage(currentSysEx.toByteArray(), currentSysEx.size())));
                } else if (expectedResponseSize == 0) {
                    currentResponse.set(customMidiEvent);
                } else {
                    log.info("Receive {} bytes, current total: {}, expected total: {}", sysexMessage
                            .getMessage().length, "0x%X".formatted(currentSysEx.size()), "0x%X".formatted(expectedResponseSize));
                }
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

    private MidiPresetIdentity getStandardPreset(CancelNotifier cancelNotifier, MidiDeviceDefinition device, MidiDeviceMode mode, String currentBankName, MidiPreset midiPreset,
                                                 MidiRequestSequence preSequence,
                                                 MidiRequestSequence sequence,
                                                 MidiRequestSequence postSequence,
                                                 MidiOutPort out) throws InvalidMidiDataException {
        final var response = requestFields(cancelNotifier, device, mode, midiPreset, preSequence, sequence, postSequence, out);
        if (response.getPatchName() != null) {
            return new MidiPresetIdentity(mode.getName(), currentBankName, response.getPatchName(), response.getCategory());
        } else {
            return null;
        }
    }

    private MidiPresetIdentity getPredefinedPreset(StandardPresetsContainer container, MidiDeviceDefinition device, MidiDeviceMode mode, int program, MidiPreset midiPreset) {
        String bankCommand = midiPreset.getBankCommand();
        MidiDeviceBank bank = device.getBankByCommand(bankCommand)
                .orElseThrow(() -> new MidiConfigError("Bank command %s not declared in presetBank section of device '%s'".formatted(bankCommand, device.getDeviceName())));
        String presetName = container.lookup(midiPreset)
                .map(StandardPreset::name)
                .orElse("Unknown");
        MidiPresetCategory category = getCategoryFromProgram(device, mode, bank, program);
        return new MidiPresetIdentity(mode.getName(), bank.getName(), presetName, category.name());
    }

    private ExtractedFields requestFields(CancelNotifier cancelNotifier, MidiDeviceDefinition device, MidiDeviceMode mode, MidiPreset midiPreset,
                                          MidiRequestSequence preSequence,
                                          MidiRequestSequence sequence,
                                          MidiRequestSequence postSequence,
                                          MidiOutPort out) throws InvalidMidiDataException {
        sendPreSequence(device, preSequence, out);
        ExtractedFields response = null;
        expectedResponseSize = sequence.getTotalSize();
        if (expectedResponseSize == 0) {
            log.warn("Response size is unknown, this will considerably slow down the extraction... Try to put response sizes in your macros");
        }
        for (var request : sequence.getMidiRequests()) {
            List<CustomMidiEvent> requestInstances = MidiEventBuilder.parse(injectConstants(midiPreset, request));
            for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                final var customMidiEvent = requestInstances.get(requestInstanceIndex);
                CustomMidiEvent midiResponse = null;
                for (int retry = 0; retry < 4; retry++) {
                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValuesSpaced());
                    checkIfShouldStop(cancelNotifier);
                    response = new ExtractedFields();
                    resetCurrentResponse();
                    out.send(customMidiEvent);
                    try {
                        midiResponse = waitResponse();
                        int receivedSize = midiResponse.getMessage()
                                .getLength();
                        log.info("Received {} bytes (${})", receivedSize, "%X".formatted(receivedSize));
                        dumpResponse(midiResponse);
                        if (sequence.getTotalSize() == 0 || receivedSize == sequence.getTotalSize()) {
                            break;
                        } else {
                            log.error("Wrong size received: {} bytes instead of {}", receivedSize, sequence.getTotalSize());
                            break;
                        }
                    } catch (MidiDeviceTimeout timeout) {
                        log.warn("No response after {} seconds, Retry...", timeout.getTimeoutInSec());
                    }
                }
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

    private String injectConstants(MidiPreset midiPreset, MidiRequest request) {
        String input = request.getValue();
        if (input.contains("program")) {
            input = input.replace("program", "%02X".formatted(midiPreset.getBankPrg()));
        }
        if (input.contains("bankMSB")) {
            input = input.replace("bankMSB", "%02X".formatted(midiPreset.getBankMSB()));
        }
        if (input.contains("bankLSB")) {
            input = input.replace("bankLSB", "%02X".formatted(midiPreset.getBankLSB()));
        }
        if (input.contains("kobjId")) {
            input = input.replace("kobjId", "%04X".formatted(midiPreset.getKurzweilObjectId()));
        }
        return input;
    }

    private void sendPreSequence(MidiDeviceDefinition device, MidiRequestSequence preSequence, MidiOutPort out) {
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
                                log.info("Received {} bytes (${})", receivedSize, "%X".formatted(receivedSize));
                            }
                        });
            }
            wait("Wait pre sequence done", device.getModeLoadTimeMs());
        }
    }

    private void sendPostSequence(MidiDeviceDefinition device, MidiRequestSequence postSequence, MidiOutPort out) {
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
                                log.info("Received {} bytes (${})", receivedSize, "%X".formatted(receivedSize));
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
