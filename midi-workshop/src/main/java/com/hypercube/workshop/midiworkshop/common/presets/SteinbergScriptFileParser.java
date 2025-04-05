package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class SteinbergScriptFileParser {
    private final MidiDeviceDefinition device;
    private final File scriptFile;
    private final Pattern ENTRY_REGEXP = Pattern.compile("^\\s*\\[([^]]+)\\]?(.+)$");
    private MidiBankFormat midiBankFormat = MidiBankFormat.BANK_MSB_LSB_PRG;
    MidiPresetNumbering midiPresetNumbering = MidiPresetNumbering.FROM_ZERO;

    public List<MidiPreset> parse() {
        try {
            List<MidiPreset> presetList = new ArrayList<>();
            for (String line : Files.readAllLines(scriptFile.toPath())) {
                var m = ENTRY_REGEXP.matcher(line);
                if (m.matches()) {
                    String command = m.group(1);
                    String body = m.group(2);
                    if (command.startsWith("parser version")) {
                        continue;
                    } else if (command.startsWith("p")) {
                        parsePreset(command, body).ifPresentOrElse(presetList::add, () -> log.warn("Unable to parse: " + line));
                    }
                }
            }
            return presetList;
        } catch (IOException e) {
            throw new MidiConfigError("Unable to read parse script file:" + scriptFile.getAbsolutePath(), e);
        }
    }

    private Optional<MidiPreset> parsePreset(String command, String body) {
        String[] parts = command.split(",");
        if (parts.length == 4) {
            int program = Integer.parseInt(parts[1].trim(), 10);
            int bankSelectMSB = Integer.parseInt(parts[2].trim(), 10);
            int bankSelectLSB = Integer.parseInt(parts[3].trim(), 10);
            String presetName = body.trim();
            return Optional.of(MidiPreset.of(presetName, device, bankSelectMSB, bankSelectLSB, program));
        }
        return Optional.empty();
    }
}
