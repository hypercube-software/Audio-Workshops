package com.hypercube.workshop.midiworkshop.api.sysex.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategory;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetCategoryType;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetDomain;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.*;
import com.hypercube.workshop.midiworkshop.api.sysex.library.request.MidiRequest;
import com.hypercube.workshop.midiworkshop.api.sysex.library.response.MidiResponseMapper;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExTemplate;
import com.hypercube.workshop.midiworkshop.api.sysex.yaml.deserializer.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Midi devices library store all known settings for a given device. This make project settings small and not redundant
 * <lu>
 * <li>By default, it is located in the same folder as the JAR or the EXE</li>
 * <li>You can override this using the environment variable MDL_FOLDER</li>
 * </lu>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MidiDeviceLibrary {
    public static final String DEVICES_LIBRARY_FOLDER = "/devices-library/";
    public static final String ENV_MDL_FOLDER = "MDL_FOLDER";
    public static final String MACRO_CALL_SEPARATOR = ";";
    private static final Pattern REGEXP_HEXA_NUMBER = Pattern.compile("(0x|\\$)?(?<value>[0-9A-F]+)");
    @Getter
    private Map<String, MidiDeviceDefinition> devices = new HashMap<>();
    private Map<Long, MidiDeviceDefinition> devicesPerNetworkId = new ConcurrentHashMap<>();
    @Getter
    private boolean loaded;

    private static MidiResponseMapper getMacroMapper(MidiDeviceDefinition device, CommandMacro macro) {
        return device.getMapper(macro.getMapperName())
                .orElseThrow(() -> new MidiConfigError("Unknown mapper '%s' for device '%'".formatted(macro.getMapperName(), device.getDeviceName())));
    }

    public void sendCommandToDevice(MidiDeviceDefinition device, MidiOutDevice midiOutDevice, CommandCall command) {
        sendCommandToDevice(device, midiOutDevice, List.of(command));
    }

    public byte[] sendCommandToDevice(MidiDeviceDefinition device, MidiOutDevice midiOutDevice, List<CommandCall> commands) {
        try (ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream()) {
            for (CommandCall commandCall : commands) {
                CommandMacro commandMacro = device.getMacro(commandCall);
                responseBuffer.write(sendCommandToDevice(device, midiOutDevice, commandMacro, commandCall));
            }
            return responseBuffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] sendCommandToDevice(MidiDeviceDefinition device, MidiOutDevice midiOutDevice, CommandMacro macro, CommandCall commandCall) {
        try (ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream()) {
            for (MidiRequest r : forgeMidiRequestSequence(device.getDefinitionFile(), device.getDeviceName(), macro, commandCall).getMidiRequests()) {
                for (CustomMidiEvent evt : MidiEventBuilder.parse(r.getValue())) {
                    log.info("Send 0x %s to %s".formatted(evt.getHexValuesSpaced(), device.getDeviceName()));
                    midiOutDevice.send(evt);
                    responseBuffer.write(evt.getMessage()
                            .getMessage());
                }
            }
            return responseBuffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void load(File applicationFolder) {
        devices.clear();
        devicesPerNetworkId.clear();
        File libraryFolder = Optional.ofNullable(System.getenv(ENV_MDL_FOLDER))
                .map(File::new)
                .orElse(new File(applicationFolder.getAbsolutePath(), DEVICES_LIBRARY_FOLDER));
        File libraryFolder2 = new File(applicationFolder.getParentFile()
                .getAbsolutePath(), DEVICES_LIBRARY_FOLDER);
        if (!libraryFolder.exists() && libraryFolder2.exists()) {
            libraryFolder = libraryFolder2;
        }
        if (libraryFolder.exists()) {
            log.info("Loading midi devices library from %s...".formatted(libraryFolder.toString()));
            try (Stream<File> midiDeviceDefinitionStream = Files.walk(libraryFolder.toPath())
                    .filter(p -> p.getFileName()
                            .toString()
                            .endsWith(".yml") || p.getFileName()
                            .toString()
                            .endsWith(".yaml"))
                    .filter(p -> !p.getFileName()
                            .toString()
                            .equals("Template.yml"))
                    .sorted(Comparator.comparing(Path::getFileName)
                            .reversed())
                    .map(Path::toFile)) {
                List<File> orderedDefinitions = midiDeviceDefinitionStream.toList();
                orderedDefinitions.forEach(file -> {
                    MidiDeviceDefinition m = loadMidiDeviceDefinition(devices, file);
                    var d = devices.get(m.getDeviceName());
                    if (d != null) {
                        d = mergeDevices(d, m);
                    } else {
                        d = m;
                    }
                    devices.put(m.getDeviceName(), d);
                });

            } catch (IOException e) {
                throw new MidiConfigError("Unable to read library folder:" + libraryFolder.toString());
            }
            log.info("%d devices defined: %s".formatted(devices.size(), getDevicesNames()));
            loaded = true;
            setControllerSysExTemplate();
        } else {
            throw new MidiConfigError("The library path does not exists: " + libraryFolder.toString());
        }
    }

    public void collectCustomPatches(MidiDeviceDefinition midiDeviceDefinition) {
        File root = new File(midiDeviceDefinition.getDefinitionFile()
                .getParentFile(), midiDeviceDefinition.getDeviceName());
        if (root.exists() && root.isDirectory()) {
            try (Stream<Path> sysExFiles = Files.walk(root.toPath())
                    .filter(p -> p.getFileName()
                            .toString()
                            .endsWith(".syx"))) {
                sysExFiles.forEach(patchPath -> {
                    Path relativePatchPath = root.toPath()
                            .relativize(patchPath);
                    if (relativePatchPath.getNameCount() == 3) {
                        String modeName = relativePatchPath.getName(0)
                                .toString();
                        String bankName = relativePatchPath.getName(1)
                                .toString();
                        String patchName = relativePatchPath.getName(2)
                                .toString()
                                .replace(".syx", "");
                        var mode = midiDeviceDefinition.getDeviceModes()
                                .get(modeName);
                        if (mode != null) {
                            var bank = mode.getBanks()
                                    .get(bankName);
                            if (bank == null) {
                                bank = new MidiDeviceBank();
                                bank.setName(bankName);
                                mode.getBanks()
                                        .put(bankName, bank);
                            }
                            String presetName = "@" + patchPath.getFileName()
                                    .toString();
                            MidiDevicePreset preset = MidiDevicePreset.of(patchPath.toFile(), midiDeviceDefinition.getPresetFormat(), presetName);
                            if (!bank.getPresets()
                                    .contains(preset)) {
                                bank.getPresets()
                                        .add(preset);
                            }
                        }
                    }
                });
            } catch (IOException e) {
                throw new MidiConfigError("Unable to read library folder:" + root.toString());
            }
        }
    }

    /**
     * Resolve recursively all macro calls in the command call. This mean we can do macros calling macros
     *
     * @param device      device owning the macro
     * @param commandCall command to expand
     * @return expanded command with its path
     */
    public List<MidiRequest> expand(File configFile, MidiDeviceDefinition device, MidiResponseMapper mapper, String commandCall) {
        return expandWithPath(device, configFile, mapper, null, commandCall, "");
    }

    /**
     * Expand a payloadBody using macro for a specific device
     *
     * @param device             device owning the macro
     * @param configFile         from where device macro are loaded
     * @param parentResponseSize optional response size if known (null otherwise)
     * @param mapper             mapper used to read the response
     * @param payloadBody        current payload to expand. Typically, contains a list of macro calls
     * @param path               keep the context of all resolved macros for debug
     * @return list of requests produced by payloadBody
     */
    public List<MidiRequest> expandWithPath(MidiDeviceDefinition device, File configFile, MidiResponseMapper mapper, Integer parentResponseSize, String payloadBody, String path) {
        checkLoaded();
        log.trace("Expand " + payloadBody);
        boolean hasMacroCall = payloadBody.contains("(");
        if (hasMacroCall) {
            return CommandCall.parse(configFile, payloadBody)
                    .stream()
                    .flatMap(commandCall -> {
                        String newPath = path + "/" + commandCall.name();
                        CommandMacro macro = device.getMacro(commandCall);
                        String expanded = macro.expand(commandCall);
                        Integer newResponseSize = macro.getResponseSize() != null ? macro.getResponseSize() : parentResponseSize;
                        MidiResponseMapper newMapper = macro.getMapperName() != null ? getMacroMapper(device, macro) : mapper;
                        // the expanded macro can contain again a list of macro commandCall, so we recurse after splitting with ";"
                        return Arrays.stream(expanded.split(MACRO_CALL_SEPARATOR))
                                .flatMap(expandedCommand -> expandWithPath(device, configFile, newMapper, newResponseSize, expandedCommand, newPath).stream());
                    })
                    .toList();
        } else {
            // There is no more macro commandCall to resolve we return the string "as is" with its path
            return List.of(new MidiRequest(path, payloadBody, parentResponseSize, mapper));
        }
    }

    public Optional<MidiDeviceDefinition> getDevice(String deviceName) {
        checkLoaded();
        return Optional.ofNullable(devices.get(deviceName));
    }

    public Optional<MidiDeviceDefinition> getDeviceFromMidiPort(String midiPort) {
        checkLoaded();
        return devices.values()
                .stream()
                .filter(def -> midiPort
                        .equals(def.getOutputMidiDevice()) || midiPort
                        .equals(def.getInputMidiDevice()))
                .findFirst();
    }

    /**
     * Given a CommandMacro without parameters, expand it to get the list of MIDI messages
     *
     * @param configFile   Where this macro is defined (used to display user-friendly error messages)
     * @param deviceName   If the macro contains macro calls, in which device we have to look for in the library
     * @param commandMacro typically something like "getAll() : --- : A();B();C()"
     * @return The list of MIDI messages payloads
     */
    public MidiRequestSequence forgeMidiRequestSequence(File configFile, String deviceName, CommandMacro commandMacro, CommandCall commandCall) {
        MidiDeviceDefinition device = Optional.ofNullable(devices.get(deviceName))
                .orElseThrow(() -> new MidiConfigError("Device not found in library: %s, did you made a typo ? Known devices are: %s".formatted(deviceName, getDevicesNames())));
        // if the mapper name is not set, mapper is null
        // if the mapper name is specified, we are looking for the corresponding MidiResponseMapper or raise an error if not found
        MidiResponseMapper mapper = Optional.ofNullable(commandMacro.getMapperName())
                .map(mapperName -> device.getMapper(mapperName)
                        .orElseThrow(() -> new MidiConfigError("Undefined request Mapper: '" + commandMacro.getMapperName() + "' in " + configFile.toString())))
                .orElse(null);
        List<MidiRequest> result = Arrays.stream(commandMacro.expand(commandCall)
                        .split(MACRO_CALL_SEPARATOR))
                .flatMap(
                        rawText -> expand(configFile, device, mapper, rawText).stream()
                )
                .toList();
        Integer totalSize = computeTotalSize(commandMacro, result);
        return new MidiRequestSequence(totalSize, result);
    }

    public MidiDeviceDefinition getDeviceByNetworkId(long networkId) {
        return devicesPerNetworkId.computeIfAbsent(networkId, key -> devices.values()
                .stream()
                .filter(d -> d.matchNetworkId(networkId))
                .findFirst()
                .orElseThrow(() -> new MidiConfigError("Device with network id %4X not found".formatted(networkId))));
    }

    /**
     * The {@link MidiPresetCategory#UNKNOWN} category must always be defined. We add it if it is missing
     */
    private void addUnknownCategory(MidiDeviceMode mode) {
        if (mode.getCategories()
                .stream()
                .filter(c -> c.name()
                        .equals(MidiPresetCategory.UNKNOWN))
                .findFirst()
                .isEmpty()) {
            ArrayList<MidiPresetCategory> categories = new ArrayList<>(mode.getCategories());
            categories.add(new MidiPresetCategory(MidiPresetCategory.UNKNOWN, MidiPresetCategoryType.REGULAR, List.of()));
            mode.setCategories(categories);
        }
    }

    private void setMappersName(MidiDeviceDefinition midiDeviceDefinition) {
        midiDeviceDefinition.getMappers()
                .forEach((mapperName, midiResponseMapper) -> {
                    midiResponseMapper.setName(mapperName);
                    midiResponseMapper.setDevice(midiDeviceDefinition);
                    midiResponseMapper.getFields()
                            .forEach((fieldName, field) -> {
                                field.setName(fieldName);
                            });
                });
    }

    private void setModeAndBankNames(MidiDeviceDefinition midiDeviceDefinition) {
        Map<String, MidiDeviceMode> deviceModes = midiDeviceDefinition.getDeviceModes();
        deviceModes
                .forEach((modeName, mode) -> {
                    mode.setName(modeName);
                    // mode inherit from device categories unless they are defined
                    if (mode.getCategories() == null || mode.getCategories()
                            .isEmpty()) {
                        mode.setCategories(midiDeviceDefinition.getCategories());
                    }
                    addUnknownCategory(mode);
                    mode.getBanks()
                            .forEach((bankName, bank) -> {
                                bank.setName(bankName);
                            });
                });
        deviceModes
                .values()
                .stream()
                .filter(m -> m.getSubBanks() != null)
                .forEach(mode -> {
                    var subBanks = mode.getSubBanks();
                    String subBankMode = subBanks.getFromMode();
                    if (deviceModes.containsKey(subBankMode)) {
                        subBanks.setMode(deviceModes
                                .get(subBankMode));
                    } else {
                        throw new MidiConfigError("Unknown mode '%s' in subBank of mode '%s'".formatted(subBankMode, mode.getName()));
                    }
                });
    }

    private String getDevicesNames() {

        return devices.isEmpty() ? "empty" : devices.keySet()
                .stream()
                .sorted()
                .collect(Collectors.joining(","));
    }

    /**
     * Merge the user settings with the initial ones
     *
     * @param org  original settings chipped with the tool
     * @param user custom settings defined by the end user
     * @return merged setting
     */
    private MidiDeviceDefinition mergeDevices(MidiDeviceDefinition org, MidiDeviceDefinition user) {
        if (user.getInputMidiDevice() != null) {
            org.setInputMidiDevice(user.getInputMidiDevice());
        }
        if (user.getOutputMidiDevice() != null) {
            org.setOutputMidiDevice(user.getOutputMidiDevice());
        }
        if (user.getInactivityTimeoutMs() != null) {
            org.setInactivityTimeoutMs(user.getInactivityTimeoutMs());
        }
        if (user.getOutputBandWidth() != null) {
            org.setOutputBandWidth(user.getOutputBandWidth());
        }
        if (user.getSysExPauseMs() != null) {
            org.setSysExPauseMs(user.getSysExPauseMs());
        }
        if (user.getDawMidiDevice() != null) {
            org.setDawMidiDevice(user.getDawMidiDevice());
        }
        for (var mode : user.getDeviceModes()
                .values()) {
            if (!org.getDeviceModes()
                    .containsKey(mode.getName())) {
                org.getDeviceModes()
                        .put(mode.getName(), mode);
            } else {
                mergeMode(org.getDeviceModes()
                        .get(mode.getName()), mode);
            }
        }
        var notOverridedMacros = org.getMacros()
                .stream()
                .filter(m -> user.getMacros()
                        .stream()
                        .filter(um -> um.name()
                                .equals(m.name()))
                        .findFirst()
                        .isEmpty());
        org.setMacros(Stream.concat(notOverridedMacros, user.getMacros()
                        .stream())
                .toList());
        return org;
    }

    private void mergeMode(MidiDeviceMode org, MidiDeviceMode current) {
        for (var bank : current.getBanks()
                .values()) {
            if (!org.getBanks()
                    .containsKey(bank.getName())) {
                org.getBanks()
                        .put(bank.getName(), bank);
            } else {
                mergeBank(org.getBanks()
                        .get(bank.getName()), bank);
            }
        }
    }

    private void mergeBank(MidiDeviceBank org, MidiDeviceBank current) {
        for (var preset : current.getPresets()) {
            if (!org.getPresets()
                    .contains(preset)) {
                org.getPresets()
                        .add(preset);
            }
        }
    }

    private void checkLoaded() {
        if (!loaded) {
            throw new MidiConfigError("MidiDeviceLibrary not loaded");
        }
    }

    /**
     * Load the YAML file dedicated to a specific midi device in the library
     *
     * @param devices
     * @param midiDeviceFile Configuration file for the device (in YAML)
     * @return the definition including macros
     */
    private MidiDeviceDefinition loadMidiDeviceDefinition(Map<String, MidiDeviceDefinition> devices, File midiDeviceFile) {
        log.debug("Load " + midiDeviceFile.toString());
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(midiDeviceFile));
            module.addDeserializer(MidiPresetCategory.class, new MidiPresetCategoryDeserializer());
            module.addDeserializer(MidiDevicePreset.class, new MidiDevicePresetDeserializer(devices, midiDeviceFile));
            module.addDeserializer(MidiDeviceController.class, new MidiDeviceControllerDeserializer());
            module.addDeserializer(MidiPresetDomain.class, new MidiPresetDomainDeserializer(midiDeviceFile));
            module.addDeserializer(int.class, new IntegerDeserializer());

            mapper.registerModule(module);
            MidiDeviceDefinition midiDeviceDefinition = mapper.readValue(midiDeviceFile, MidiDeviceDefinition.class);
            // cleanup: remove null elements
            midiDeviceDefinition.setMacros(Optional.ofNullable(midiDeviceDefinition.getMacros())
                    .map(macros -> macros
                            .stream()
                            .filter(m -> m != null)
                            .toList())
                    .orElse(List.of()));
            setMappersName(midiDeviceDefinition);
            setModeAndBankNames(midiDeviceDefinition);
            midiDeviceDefinition.setDefinitionFile(midiDeviceFile);
            setRemoteMidiPorts(midiDeviceDefinition);
            return midiDeviceDefinition;
        } catch (IOException e) {
            throw new MidiConfigError("Unable to load " + midiDeviceFile.toString(), e);
        }
    }

    /**
     * Complete remote midi ports with the name of the device
     */
    private void setRemoteMidiPorts(MidiDeviceDefinition midiDeviceDefinition) {
        String outputMidiDevice = midiDeviceDefinition.getOutputMidiDevice();
        if (outputMidiDevice != null) {
            var count = outputMidiDevice.split(":", -1).length - 1;
            if (count == 1) {
                outputMidiDevice = "%s:%s".formatted(outputMidiDevice, midiDeviceDefinition.getDeviceName());
                midiDeviceDefinition.setOutputMidiDevice(outputMidiDevice);
                midiDeviceDefinition.setInputMidiDevice(outputMidiDevice);
            }
        }
    }

    /**
     * Resolve all controllers macros
     */
    private void setControllerSysExTemplate() {
        for (MidiDeviceDefinition midiDeviceDefinition : devices.values()) {
            midiDeviceDefinition.getControllers()
                    .stream()
                    .filter(c -> c.getType() == ControllerValueType.SYSEX)
                    .forEach(c -> {
                        List<CommandCall> commandCalls = CommandCall.parse(midiDeviceDefinition.getDefinitionFile(), c.getIdentity());
                        if (commandCalls.size() != 1) {
                            throw new IllegalArgumentException("Expected 1 CommandCall, got " + commandCalls.size());
                        }
                        CommandCall commandCall = commandCalls.getFirst();
                        List<MidiRequest> expandedTexts = expand(midiDeviceDefinition.getDefinitionFile(), midiDeviceDefinition, null, c.getIdentity());
                        if (expandedTexts.size() != 1) {
                            throw new IllegalArgumentException("Expected 1 MidiRequest, got " + expandedTexts.size() + " expanding controller " + c.getIdentity());
                        }
                        c.setSysExTemplate(SysExTemplate.of(expandedTexts.getFirst()));
                    });
        }
    }

    /**
     * If the macro define a response size, and we can also compute it, check they match
     */
    private Integer computeTotalSize(CommandMacro commandMacro, List<MidiRequest> result) {
        int sum = 0;
        for (var request : result) {
            if (request.getResponseSize() == null) {
                sum = 0;
                break;
            }
            sum += request.getResponseSize();
        }
        if (sum != 0 && commandMacro.getResponseSize() != null && sum != commandMacro.getResponseSize()) {
            throw new MidiConfigError("The sum of all requests $%X does not match the macro response size: $%X".formatted(sum, commandMacro.getResponseSize()));
        } else if (sum != 0) {
            return sum;
        } else if (commandMacro.getResponseSize() != null) {
            return commandMacro.getResponseSize();
        }
        return null;
    }


}
