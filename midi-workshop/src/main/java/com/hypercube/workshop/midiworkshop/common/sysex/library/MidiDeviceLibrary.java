package com.hypercube.workshop.midiworkshop.common.sysex.library;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.workshop.midiworkshop.common.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.midiworkshop.common.sysex.yaml.CommandMacroDeserializer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Midi device library store all known settings for a given device. This make project settings small and not redundant
 * <lu>
 * <li>By default, it is located in the same folder as the JAR or the EXE</li>
 * <li>You can override this using the environment variable MDL_FOLDER</li>
 * </lu>
 */
@Service
@Slf4j
public class MidiDeviceLibrary {
    public static final String DEVICES_LIBRARY_FOLDER = "/devices-library/";
    public static final String ENV_MDL_FOLDER = "MDL_FOLDER";
    private static final Pattern REGEXP_HEXA_NUMBER = Pattern.compile("(0x|\\$)?(?<value>[0-9A-F]+)");

    private Map<String, MidiDeviceDefinition> devices;
    @Getter
    private boolean loaded;


    public void load(File applicationFolder) {
        devices = new HashMap<>();
        File libraryFolder = Optional.ofNullable(System.getenv(ENV_MDL_FOLDER))
                .map(File::new)
                .orElse(new File(applicationFolder.getAbsolutePath(), DEVICES_LIBRARY_FOLDER));
        File libraryFolder2 = new File(applicationFolder.getParentFile()
                .getAbsolutePath(), DEVICES_LIBRARY_FOLDER);
        if (!libraryFolder.exists() && libraryFolder2.exists()) {
            libraryFolder = libraryFolder2;
        }
        if (libraryFolder.exists()) {
            log.info("Loading midi device library from %s...".formatted(libraryFolder.toString()));
            try (Stream<MidiDeviceDefinition> midiDeviceDefinitionStream = Files.walk(libraryFolder.toPath())
                    .filter(p -> p.getFileName()
                            .toString()
                            .endsWith(".yml"))
                    .sorted(Comparator.comparing(Path::getFileName)
                            .reversed())
                    .map(Path::toFile)
                    .map(this::loadDeviceMacro)) {
                midiDeviceDefinitionStream
                        .forEach(m -> {
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
        } else {
            throw new MidiConfigError("The library path does not exists: " + libraryFolder.toString());
        }
    }

    private String getDevicesNames() {

        return devices.size() == 0 ? "empty" : devices.keySet()
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

    /**
     * Resolve recursively all macro calls in the command call. This mean we can do macros calling macros
     *
     * @param deviceName  in which device we have to look for in the library
     * @param commandCall command to expand
     * @return expanded command with its path
     */
    public List<ExpandedRequestDefinition> expand(File configFile, String deviceName, String commandCall) {
        checkLoaded();
        return expandWithPath(configFile, deviceName, commandCall, "");
    }

    public List<ExpandedRequestDefinition> expandWithPath(File configFile, String deviceName, String commandCall, String path) {
        checkLoaded();
        log.trace("Expand " + commandCall);
        var matches = Optional.ofNullable(devices.get(deviceName))
                .map(d -> d.getMacros()
                        .stream()
                        .filter(m -> m.matches(commandCall))
                        .toList())
                .orElseThrow(() -> new MidiConfigError("Device not found in library: %s, did you made a typo ? Known devices are: %s".formatted(deviceName, getDevicesNames())));
        log.trace("Found " + matches.size() + " macros");
        if (matches.isEmpty() && commandCall.contains("(")) {
            throw new MidiConfigError("Undefined macro for device %s: %s".formatted(deviceName, commandCall));
        }
        if (matches.size() == 1) {
            CommandCall call = CommandCall.parse(configFile, commandCall);
            String expanded = matches.getFirst()
                    .expand(call);
            return Arrays.stream(expanded.split(";"))
                    .flatMap(expandedCommand -> expandWithPath(configFile, deviceName, "%s : %s".formatted(call.name(), expandedCommand), path + "/" + call.name()).stream())
                    .toList();
        } else if (matches.size() > 1) {
            String msg = matches.stream()
                    .map(CommandMacro::toString)
                    .collect(Collectors.joining("\n"));
            throw new MidiConfigError("Ambiguous macro call, multiple name are available in" + commandCall + "\n" + msg);
        } else {
            // There is no more macro call to resolve we return the string "as is" with its path
            return List.of(new ExpandedRequestDefinition(path, commandCall));
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
                .filter(def -> def.getOutputMidiDevice()
                        .equals(midiPort) || def.getInputMidiDevice()
                        .equals(midiPort))
                .findFirst();
    }

    private void checkLoaded() {
        if (!loaded) {
            throw new MidiConfigError("MidiDeviceLibrary not loaded");
        }
    }

    /**
     * Load the YAML file dedicated to a specific midi device in the library
     *
     * @param midiDeviceFile Configuration file for the device (in YAML)
     * @return the definition including macros
     */
    private MidiDeviceDefinition loadDeviceMacro(File midiDeviceFile) {
        log.debug("Load " + midiDeviceFile.toString());
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(midiDeviceFile));
            mapper.registerModule(module);
            MidiDeviceDefinition macro = mapper.readValue(midiDeviceFile, MidiDeviceDefinition.class);
            // cleanup: remove null elements
            macro.setMacros(Optional.ofNullable(macro.getMacros())
                    .map(macros -> macros
                            .stream()
                            .filter(m -> m != null)
                            .toList())
                    .orElse(List.of()));
            return macro;
        } catch (IOException e) {
            throw new MidiConfigError("Unable to load " + midiDeviceFile.toString(), e);
        }
    }

    /**
     * Given a CommandMacro without parameters, expand it to get the list of MIDI messages
     *
     * @param configFile   Where this macro is defined (used to display user-friendly error messages)
     * @param deviceName   If the macro contains macro calls, in which device we have to look for in the library
     * @param commandMacro typically something like "getAll() : --- : A();B();C()"
     * @return The list of MIDI messages payloads
     */
    public MidiRequestSequence forgeMidiRequestSequence(File configFile, String deviceName, CommandMacro commandMacro) {
        if (commandMacro.parameters()
                .size() != 0) {
            throw new MidiConfigError("Can't expand a macro with parameters");
        }
        var result = Arrays.stream(commandMacro.body()
                        .split(";"))
                .flatMap(
                        rawText -> expandRequestDefinition(configFile, deviceName, rawText)
                )
                .toList();
        return new MidiRequestSequence(result);
    }

    private Stream<MidiRequest> expandRequestDefinition(File configFile, String deviceName, String rawText) {
        // expand the macros calls if there are any
        List<ExpandedRequestDefinition> expandedTexts = expand(configFile, deviceName, rawText);
        // the final string should be "<command name> : <size> : <bytes>"
        return expandedTexts.stream()
                .map(expandedRequestDefinition -> {
                    String[] values = expandedRequestDefinition.payload()
                            .split(":");
                    if (values.length == 3) {
                        Integer size = parseOptionalSize(values);
                        String name = values[0];
                        String value = values.length == 3 ? values[2] : values[1];

                        return new MidiRequest(expandedRequestDefinition.path()
                                .trim(), value
                                .trim(), size);
                    } else if (values.length == 2) {
                        Integer size = parseOptionalSize(values);
                        String name = "";
                        String value = values.length == 3 ? values[2] : values[1];

                        return new MidiRequest(expandedRequestDefinition.path()
                                .trim(), value
                                .trim(), size);
                    } else {
                        throw new MidiConfigError("Unexpected Bulk Request definition, should have 3 section <name>:<size>:<payload>: \"%s\"\nMay be a macro is not resolved.".formatted(expandedRequestDefinition.payload()));
                    }
                });
    }

    /**
     * Size in macro definitions are always in hexadecimal and optional
     *
     * @param values a hexadecimal number or something else like "---"
     * @return null if the size is not an hexadecimal number
     */
    private Integer parseOptionalSize(String[] values) {
        Integer size = null;
        Matcher m = REGEXP_HEXA_NUMBER.matcher(values[1]);
        if (m.find()) {
            size = Integer.parseInt(m.group("value")
                    .trim(), 16);
        }
        return size;
    }
}
