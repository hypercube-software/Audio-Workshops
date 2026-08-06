package com.hypercube.workshop.midiworkshop.api.presets.standard;

import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardBank;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardBankId;
import com.hypercube.workshop.midiworkshop.api.presets.standard.model.StandardPreset;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public abstract class StandardPresetsContainer {
    private static final Pattern DRUM_NOTE_PATTERN = Pattern.compile("^\\s+(?<note>\\d+)\\s+(?<title>.+)$");

    private final List<StandardPreset> presets;
    private final List<StandardBank> banks;

    protected StandardPresetsContainer(String banksFile, String presetsFile) {
        this.banks = loadRawLines(banksFile).stream()
                .map(this::toStandardBank)
                .flatMap(Optional::stream)
                .toList();
        this.presets = loadPresets(presetsFile);
    }

    public Optional<StandardPreset> lookup(MidiPreset midiPreset) {
        return presets.stream()
                .filter(preset -> preset.matches(midiPreset))
                .findFirst();
    }

    public StandardBank lookupBank(StandardBankId bankId) {
        return banks.stream()
                .filter(b -> b.bankId()
                        .equals(bankId))
                .findFirst()
                .orElseThrow(() -> new MidiConfigError("Bank not found: %s-%s".formatted(bankId.msb(), bankId.lsb())));
    }

    /**
     * Parses a presets file where each line is either a {@link StandardPreset} or a {@link DrumKitNote}.
     * <pre>
     * 0-0-0 Grand Piano           ← non-indented: preset definition (msb-lsb-prg name)
     * 127-0-0 Standard Kit        ← drumkit preset
     *     35 Hyper Tom L 1        ← indented: drum note (note title), added to previous preset's drumMap
     *     36 Asian Tom L          ← indented: drum note, added to previous preset's drumMap
     * </pre>
     */
    private List<StandardPreset> loadPresets(String presetsFile) {
        List<StandardPreset> result = new ArrayList<>();
        for (String line : loadRawLines(presetsFile)) {
            toStandardPreset(line).ifPresentOrElse(
                    result::add,
                    () -> parseDrumNote(line).ifPresent(dn -> result.getLast()
                            .drumMap()
                            .add(dn)));
        }
        return result;
    }

    private Optional<DrumKitNote> parseDrumNote(String line) {
        Matcher m = DRUM_NOTE_PATTERN.matcher(line);
        if (m.matches()) {
            return Optional.of(new DrumKitNote(m.group("title"), Integer.parseInt(m.group("note"))));
        }
        return Optional.empty();
    }

    private List<String> loadRawLines(String resourcePath) {
        return Optional.ofNullable(resourcePath)
                .map(rsc -> {
                    URL resource = this.getClass()
                            .getClassLoader()
                            .getResource(resourcePath);
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                        return reader.lines()
                                .toList();
                    } catch (Exception e) {
                        throw new MidiConfigError("Unable to open " + resource, e);
                    }
                })
                .orElse(List.of());
    }

    private Optional<StandardBank> toStandardBank(String entry) {
        try {
            return Optional.of(StandardBank.of(entry));
        } catch (MidiConfigError e) {
            return Optional.empty();
        }
    }

    private Optional<StandardPreset> toStandardPreset(String entry) {
        try {
            return Optional.of(StandardPreset.of(entry));
        } catch (MidiConfigError e) {
            return Optional.empty();
        }
    }
}
