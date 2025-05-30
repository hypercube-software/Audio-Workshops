package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNaming;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;
import com.hypercube.workshop.midiworkshop.common.sysex.library.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class MidiDeviceDefinition {
    private File definitionFile;
    private String deviceName;
    private String brand;
    private String inputMidiDevice;
    private String outputMidiDevice;
    private Integer outputBandWidth;
    private Integer sysExPauseMs;
    private Integer inactivityTimeoutMs;
    private MidiBankFormat presetFormat;
    private MidiPresetNumbering presetNumbering;
    private MidiPresetNaming presetNaming;
    private int presetLoadTimeMs = 1000;
    private int modeLoadTimeMs = 1000;
    private MidiDeviceDecodingKey decodingKey;
    private List<CommandMacro> macros = new ArrayList<>();
    private List<String> presets = new ArrayList<>();
    private List<MidiPresetCategory> categories = new ArrayList<>();
    private Map<String, MidiDeviceMode> deviceModes = new HashMap<>();
    private Map<String, MidiResponseMapper> mappers = new HashMap<>();

    /**
     * Given a command call, tell if some macros matches the call
     *
     * @param commandCall The command call
     * @return matched macros
     */
    public List<CommandMacro> matchMacros(CommandCall commandCall) {
        return macros
                .stream()
                .filter(m -> m.matches(commandCall))
                .toList();
    }

    /**
     * Look for a macro for a given macro call.
     *
     * @throws MidiConfigError if the macro is not found or there is more than 1 match
     */
    public CommandMacro getMacro(CommandCall commandCall) {
        var matches = matchMacros(commandCall);
        log.trace("Found " + matches.size() + " macros");
        if (matches.isEmpty()) {
            throw new MidiConfigError("Undefined macro for device %s: %s".formatted(deviceName, commandCall.toString()));
        } else if (matches.size() > 1) {
            String msg = matches.stream()
                    .map(CommandMacro::toString)
                    .collect(Collectors.joining("\n"));
            throw new MidiConfigError("Ambiguous macro commandCall, multiple name are available for " + commandCall.toString() + "\n" + msg);
        }
        return matches.getFirst();
    }

    public MidiPresetCategory getCategory(MidiDeviceMode mode, int categoryIndex) {
        if (categoryIndex >= 0 && categoryIndex < mode.getCategories()
                .size()) {
            return mode.getCategories()
                    .get(categoryIndex);
        } else {
            return MidiPresetCategory.of("Unknown " + categoryIndex);
        }
    }

    public Optional<MidiDeviceBank> getBank(String bankName) {
        for (var mode : getDeviceModes().values()) {
            for (var bank : mode.getBanks()
                    .values()) {
                if (bank.getName()
                        .equals(bankName)) {
                    return Optional.of(bank);
                }
            }
        }
        return Optional.empty();
    }

    public int getBankId(String bankName) {
        String bankNumber = getBank(bankName).map(MidiDeviceBank::getCommand)
                .orElse(bankName);
        return parseBankNumber(bankNumber);
    }

    public int getCategoryCode(MidiDeviceMode mode, String categoryName) {
        if (categoryName == null) {
            return 0;
        }
        for (int c = 0; c < mode.getCategories()
                .size(); c++) {
            if (mode.getCategories()
                    .get(c)
                    .matches(categoryName)) {
                return c;
            }
        }
        return 0;
    }

    public Optional<MidiResponseMapper> getMapper(String mapperName) {
        return Optional.ofNullable(mappers.get(mapperName));
    }

    public Optional<MidiDeviceBank> getBankByMSB(String msb) {
        for (var mode : getDeviceModes().values()) {
            for (var bank : mode.getBanks()
                    .values()) {
                if (bank.getCommand()
                        .equals(msb)) {
                    return Optional.of(bank);
                }
            }
        }
        return Optional.empty();
    }

    private int parseBankNumber(String bankNameOrId) {
        try {
            if (bankNameOrId.startsWith("$")) {
                return Integer.parseInt(bankNameOrId.substring(1), 16);
            } else if (bankNameOrId.startsWith("0x")) {
                return Integer.parseInt(bankNameOrId.substring(2), 16);
            } else {
                return Integer.parseInt(bankNameOrId, 10);
            }
        } catch (NumberFormatException e) {
            throw new MidiConfigError("Bank name '%s' not defined in presetBanks section for device '%s'".formatted(bankNameOrId, deviceName));
        }
    }
}
