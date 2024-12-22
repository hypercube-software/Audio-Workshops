package com.hypercube.midi.translator.config.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.MidiTranslator;
import com.hypercube.midi.translator.config.yaml.CommandMacroDeserializer;
import com.hypercube.midi.translator.error.ConfigError;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandCall;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Midi device library store all known settings for a given device. This make project settings small and not redundant
 */
@Service
public class MidiDeviceLibrary {
    private Map<String, MidiDeviceDefinition> midiDevicesLibrary;

    /**
     * This method give us the location of the CLI no matter what is the current directory
     *
     * @return
     */
    public File getConfigFolder() {
        try {
            URI uri = MidiTranslator.class
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
                    // The application run in debug inside an IDE
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
                return f.getParentFile()
                        .getParentFile();
            }
            throw new ConfigError("Unexpected location: " + uri.toString());
        } catch (URISyntaxException e) {
            throw new ConfigError(e);
        }
    }

    public void load() {
        midiDevicesLibrary = new HashMap<>();
        Path libraryFolder = Path.of(getConfigFolder().getAbsolutePath() + "/devices/");
        try (Stream<MidiDeviceDefinition> midiDeviceDefinitionStream = Files.walk(libraryFolder)
                .filter(p -> p.getFileName()
                        .toString()
                        .endsWith(".yml"))
                .map(this::loadDeviceMacro)) {
            midiDeviceDefinitionStream
                    .forEach(m -> midiDevicesLibrary.put(m.getDeviceName(), m));

        } catch (IOException e) {
            throw new MidiError("Unable to read library folder:" + libraryFolder.toString());
        }
    }

    /**
     * Resolve recursively all macro calls in the command call. This mean we can do macros calling macros
     *
     * @param deviceName  resolve in priority macros for this device (in case of duplicate names for multiple devices)
     * @param commandCall command to expand
     * @return expanded command
     */
    public String expand(String deviceName, String commandCall) {
        var matches = Optional.ofNullable(midiDevicesLibrary.get(deviceName))
                .map(d -> d.getMacros()
                        .stream()
                        .filter(m -> m.match(commandCall))
                        .toList())
                .orElse(List.of());

        if (matches.isEmpty()) {
            matches = midiDevicesLibrary.values()
                    .stream()
                    .flatMap(mdm -> mdm.getMacros()
                            .stream())
                    .filter(m -> m.match(commandCall))
                    .toList();
        }
        if (matches.size() == 1) {
            CommandCall call = CommandCall.parse(commandCall);
            String expanded = matches.get(0)
                    .expand(call);
            return expand(deviceName, "%s : %s".formatted(call.name(), expanded));
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
        return Optional.ofNullable(midiDevicesLibrary.get(deviceName));
    }

    private MidiDeviceDefinition loadDeviceMacro(Path macroFile) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(CommandMacro.class, new CommandMacroDeserializer(macroFile));
            mapper.registerModule(module);
            MidiDeviceDefinition macro = mapper.readValue(macroFile.toFile(), MidiDeviceDefinition.class);
            // cleanup: remove null elements
            macro.setMacros(macro.getMacros()
                    .stream()
                    .filter(m -> m != null)
                    .toList());
            return macro;
        } catch (IOException e) {
            throw new ConfigError("Unable to load " + macroFile.toString(), e);
        }
    }
}
