package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.MidiWorkshopApplication;
import com.hypercube.workshop.midiworkshop.common.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.common.sysex.util.SysExBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
public class MidiPresetCrawler {
    private final Pattern SOUND_CANVAS_PRESET_DEFINITION_REGEXP = Pattern.compile("\\d+-\\d+-\\d+\\s(.+)");

    public void crawlAllPatches(String deviceName) {
        MidiDeviceLibrary library = new MidiDeviceLibrary();
        library.load(ConfigHelper.getApplicationFolder(MidiWorkshopApplication.class));
        MidiDeviceDefinition device = library.getDevice(deviceName)
                .orElse(null);

        MidiDeviceManager midiDeviceManager = new MidiDeviceManager();
        midiDeviceManager.collectDevices();
        String outputMidiDevice = device.getOutputMidiDevice();
        MidiOutDevice out = midiDeviceManager.getOutput(outputMidiDevice)
                .orElse(null);
        String inputMidiDevice = device.getInputMidiDevice();
        MidiInDevice in = midiDeviceManager.getInput(inputMidiDevice)
                .orElse(null);
        if (out == null) {
            throw new MidiConfigError("MIDI OUT Device not found: '%'".formatted(outputMidiDevice));
        }
        if (in == null) {
            throw new MidiConfigError("MIDI IN Device not found: '%'".formatted(inputMidiDevice));
        }
        var macro = device.getMacros()
                .stream()
                .filter(m -> m.name()
                        .equals("PatchName"))
                .findFirst()
                .orElse(null);
        if (macro == null) {
            throw new MidiConfigError("Macro PatchName not found: '%'".formatted(inputMidiDevice));
        }
        AtomicReference<CustomMidiEvent> reponse = new AtomicReference<>();
        MidiRequestSequence sequence = library.forgeMidiRequestSequence(device.getDefinitionFile(), deviceName, macro);
        try {
            in.open();
            in.addSysExListener((device1, event) -> {
                log.info("receive " + event.getHexValues());
                reponse.set(event);
            });
            in.startListening();
            out.open();
            for (var domain : device.getPresetDomains()) {
                for (var range : domain.ranges()) {
                    for (int program : IntStream.rangeClosed(range.getFrom(), range.getTo())
                            .toArray()) {
                        MidiPreset midiPreset = MidiPreset.of(device, domain.bank(), program);
                        log.info("Select bank " + domain.bank() + " program " + program);
                        for (var command : midiPreset.getCommands()) {
                            CustomMidiEvent cm = new CustomMidiEvent(command, 0);
                            log.info("    " + cm.getHexValues());
                            out.send(cm);
                        }
                        wait(1000);
                        reponse.set(null);
                        String presetName = "Unknown";
                        String category = "Unknown";
                        if (device.getMidiPresetNaming() == MidiPresetNaming.SOUND_CANVAS) {
                            presetName = getSoundCanvasPresetName(midiPreset.getBankMSB(), midiPreset.getBankLSB(), program).orElse(presetName);
                            category = getSoundCanvasCategory(program);
                        } else {
                            for (var request : sequence.getMidiRequests()) {
                                List<CustomMidiEvent> requestInstances = SysExBuilder.parse(request.getValue());
                                for (int requestInstanceIndex = 0; requestInstanceIndex < requestInstances.size(); requestInstanceIndex++) {
                                    var customMidiEvent = requestInstances.get(requestInstanceIndex);
                                    log.info("Request {}/{} \"{}\": {}", requestInstanceIndex + 1, requestInstances.size(), request.getName(), customMidiEvent.getHexValues());
                                    out.send(customMidiEvent);
                                }
                            }
                            wait(() -> reponse.get() != null);
                            String name = new String(reponse.get()
                                    .extractBytes(8, 0x20));
                        }
                        log.info("Patch name : " + presetName);
                        log.info("Category   : " + category);
                        log.info("");
                    }
                }
            }
            in.stopListening();
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    List<String> soundCanvasCategories = List.of(
            "Piano", "Chromatic Percussion", "Organ", "Guitar", "Bass", "Strings", "Ensemble", "Brass", "Reed", "Pipe", "Synth lead", "Synth Pad",
            "Synth SFX", "Ethnic", "Percussive", "SFX");

    private String getSoundCanvasCategory(int program) {
        int category = program / 8;
        if (category < soundCanvasCategories.size()) {
            return soundCanvasCategories.get(category);
        } else {
            return "Unknown";
        }
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
            throw new RuntimeException("Erreur lors de la lecture de l'InputStream", e); // Gestion des exceptions améliorée
        }
    }

    private Optional<String> parseEntry(String soundCanvasPresetDefinition) {
        var m = SOUND_CANVAS_PRESET_DEFINITION_REGEXP.matcher(soundCanvasPresetDefinition);
        if (m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    private static void wait(Supplier<Boolean> predicate) {
        do {
            wait(50);
        }
        while (!predicate.get());
    }

    private static void wait(int timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
