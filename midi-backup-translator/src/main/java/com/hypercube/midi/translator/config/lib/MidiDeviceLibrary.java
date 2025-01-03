package com.hypercube.midi.translator.config.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.MidiBackupTranslator;
import com.hypercube.midi.translator.config.yaml.CommandMacroDeserializer;
import com.hypercube.midi.translator.error.ConfigError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Midi device library store all known settings for a given device. This make project settings small and not redundant
 */
@Service
@Slf4j
public class MidiDeviceLibrary {
    private Map<String, MidiDeviceDefinition> devices;
    @Getter
    private boolean loaded;

    /**
     * This method give us the location of the CLI no matter what is the current directory
     *
     * @return
     */
    public File getConfigFolder() {
        try {
            URI uri = MidiBackupTranslator.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI();
            String scheme = uri.getScheme();
            if (scheme
                    .equals("file")) {
                String path = uri.getPath();
                File f = new File(path);
                if (path.endsWith("/target/classes/")) {
                    // The application run in inside an IDE
                    f = f.getParentFile();
                } else {
                    // The application run as native EXE
                }
                return f.getParentFile();
            } else if (scheme.equals("jar")) {
                // The application run in command line as an executable JAR
                String path = uri.getRawSchemeSpecificPart()
                        .replace("nested:", "")
                        .replaceAll("\\.jar.*", "");
                File f = new File(path);
                if (path.contains("/target/")) {
                    // The application run in debug inside an IDE
                    return f.getParentFile()
                            .getParentFile();
                } else {
                    return f.getParentFile();
                }
            }
            throw new ConfigError("Unexpected location: " + uri.toString());
        } catch (URISyntaxException e) {
            throw new ConfigError(e);
        }
    }

    public void load() {
        devices = new HashMap<>();
        Path libraryFolder = Path.of(getConfigFolder().getAbsolutePath() + "/devices/");
        if (libraryFolder.toFile()
                .exists()) {
            log.info("Loading midi device library from %s...".formatted(libraryFolder.toString()));
            try (Stream<MidiDeviceDefinition> midiDeviceDefinitionStream = Files.walk(libraryFolder)
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
                throw new ConfigError("Unable to read library folder:" + libraryFolder.toString());
            }
            log.info("%d devices defined: %s".formatted(devices.size(), getDevicesNames()));

            loaded = true;
        } else {
            throw new ConfigError("The library path does not exists: " + libraryFolder.toString());
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
     * @param deviceName  resolve in priority macros for this device (in case of duplicate names for multiple devices)
     * @param commandCall command to expand
     * @return expanded command
     */
    public String expand(File configFile, String deviceName, String commandCall) {
        checkLoaded();
        var matches = Optional.ofNullable(devices.get(deviceName))
                .map(d -> d.getMacros()
                        .stream()
                        .filter(m -> m.match(commandCall))
                        .toList())
                .orElseThrow(() -> new ConfigError("Device not found in library: %s, did you made a typo ? Known devices are: %s".formatted(deviceName, getDevicesNames())));

        if (matches.isEmpty() && commandCall.contains("(")) {
            throw new ConfigError("Undefined macro for device %s: %s".formatted(deviceName, commandCall));
        }
        if (matches.size() == 1) {
            CommandCall call = CommandCall.parse(configFile, commandCall);
            String expanded = matches.get(0)
                    .expand(call);
            return expand(configFile, deviceName, "%s : %s".formatted(call.name(), expanded));
        } else if (matches.size() > 1) {
            String msg = matches.stream()
                    .map(CommandMacro::toString)
                    .collect(Collectors.joining("\n"));
            throw new ConfigError("Ambiguous macro call, multiple name are available in" + commandCall + "\n" + msg);
        } else {
            return commandCall;
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
            throw new ConfigError("MidiDeviceLibrary not loaded");
        }
    }


    private MidiDeviceDefinition loadDeviceMacro(File macroFile) {
        log.debug("Load " + macroFile.toString());
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(macroFile));
            mapper.registerModule(module);
            MidiDeviceDefinition macro = mapper.readValue(macroFile, MidiDeviceDefinition.class);
            // cleanup: remove null elements
            macro.setMacros(Optional.ofNullable(macro.getMacros())
                    .map(macros -> macros
                            .stream()
                            .filter(m -> m != null)
                            .toList())
                    .orElse(List.of()));
            return macro;
        } catch (IOException e) {
            throw new ConfigError("Unable to load " + macroFile.toString(), e);
        }
    }
}
