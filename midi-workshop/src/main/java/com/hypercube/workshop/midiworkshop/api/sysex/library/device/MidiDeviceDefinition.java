package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.devices.remote.NetworkIdBuilder;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetNaming;
import com.hypercube.workshop.midiworkshop.api.sysex.library.importer.PatchOverride;
import com.hypercube.workshop.midiworkshop.api.sysex.library.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
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
    public static final String DAW_DEVICE_NAME = "DAW";
    /**
     * Where this device has been defined
     */
    private File definitionFile;
    /**
     * Name of the device
     */
    private String deviceName;
    /**
     * Device brand
     */
    private String brand;
    /**
     * Midi port receiving messages from the revice
     */
    private String inputMidiDevice;
    /**
     * Midi port sending messages to the device
     */
    private String outputMidiDevice;
    /**
     * Midi port send messages from the DAW for this device
     */
    private String dawMidiDevice;
    /**
     * Optional limit to output messages in bytes per sec.
     */
    private Integer outputBandWidth;
    /**
     * How many ms we have to way after sending a sysex
     */
    private Integer sysExPauseMs;
    /**
     * Maximum time in ms we wait the response of the device (typically during backup)
     */
    private Integer inactivityTimeoutMs;
    /**
     * How the device select patches
     */
    private MidiBankFormat presetFormat;
    /**
     * How patch names can be retreived on this device
     */
    private MidiPresetNaming presetNaming;
    /**
     * How long the device makes to load new patches in ms
     */
    private int presetLoadTimeMs = 1000;
    /**
     * How long the device makes to change mode in ms
     */
    private int modeLoadTimeMs = 1000;
    /**
     * Decoding key used to convert 7 bits SysEx to the real 8 bit payload
     */
    private MidiDeviceDecodingKey decodingKey;
    /**
     * Macros definitions for this device
     */
    private List<CommandMacro> macros = new ArrayList<>();
    /**
     * Patch categories defined globally for the device.
     * <p>They can be overriden at the mode level</p>
     */
    private List<MidiPresetCategory> categories = new ArrayList<>();
    /**
     * Which MIDI controllers are available in this device
     */
    private List<MidiDeviceController> controllers = new ArrayList<>();
    /**
     * How many modes are supported in this device (Typically Voice, Multi, Performance)
     */
    private Map<String, MidiDeviceMode> deviceModes = new HashMap<>();
    /**
     * Mappers used to extract data from SysEx response (Typically patch names and categories)
     */
    private Map<String, MidiResponseMapper> mappers = new HashMap<>();
    /**
     * Used to import SysEx files and modify them to match the Edit Buffer only
     */
    private List<PatchOverride> patchOverrides;

    public Optional<MidiDeviceMode> getMode(String mode) {
        return Optional.ofNullable(deviceModes.get(mode));
    }

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

    public Optional<MidiDeviceBank> getBankByCommand(String command) {
        for (var mode : getDeviceModes().values()) {
            for (var bank : mode.getBanks()
                    .values()) {
                if (command.equals(bank.getCommand()) || ("$" + command).equals(bank.getCommand())) {
                    return Optional.of(bank);
                }
            }
        }
        return Optional.empty();
    }

    public boolean matchNetworkId(long networkId) {
        return getDeviceNetworkId() == networkId;
    }

    public long getDeviceNetworkId() {
        return NetworkIdBuilder.getDeviceNetworkId(this.deviceName);
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
