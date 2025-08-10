package com.hypercube.midi.translator.config.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.MidiBackupTranslator;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.yaml.MidiTranslationDeserializer;
import com.hypercube.midi.translator.error.ConfigError;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiRequestSequence;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectConfigurationFactory {
    private final MidiDeviceLibrary midiDeviceLibrary;

    @Value("${mbt-config:./config.yml}")
    private File configFile;

    public void forge() {

    }

    public ProjectConfiguration loadConfig() {
        loadLibary();
        if (configFile.exists()) {
            log.info("Loading project configuration from  %s...".formatted(configFile.getAbsolutePath()));
            var mapper = new ObjectMapper(new YAMLFactory());
            try {
                SimpleModule module = new SimpleModule();
                module.addDeserializer(MidiTranslation.class, new MidiTranslationDeserializer(midiDeviceLibrary, configFile));
                mapper.registerModule(module);
                ProjectConfiguration config = mapper.readValue(configFile, ProjectConfiguration.class);
                config.getDevices()
                        .forEach(projectDevice -> {
                            setDefaultDeviceSettings(projectDevice);
                            projectDevice.setDumpRequestTemplates(
                                    forgeMidiRequestSequences(projectDevice));
                        });
                config.getTranslations()
                        .forEach(t -> config.getTranslationsMap()
                                .put(t.getCc(), t));
                return config;
            } catch (IOException e) {
                throw new ConfigError(e);
            }
        } else {
            log.warn("The project config does not exists: " + configFile.getAbsolutePath());
            return new ProjectConfiguration();
        }
    }

    private void loadLibary() {
        if (!midiDeviceLibrary.isLoaded()) {
            midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(MidiBackupTranslator.class));
        }
    }

    /**
     * If the device is defined in the library and some values are not already set, try to fill the gap with what we have
     *
     * @param device
     */
    private void setDefaultDeviceSettings(ProjectDevice device) {
        midiDeviceLibrary.getDevice(device.getName())
                .ifPresent(def -> {
                    if (device.getInactivityTimeoutMs() == null) {
                        device.setInactivityTimeoutMs(def.getInactivityTimeoutMs());
                    }
                    if (device.getSysExPauseMs() == null) {
                        device.setSysExPauseMs(def.getSysExPauseMs());
                    }
                    if (device.getOutputBandWidth() == null) {
                        device.setOutputBandWidth(def.getOutputBandWidth());
                    }
                    if (device.getInputMidiDevice() == null) {
                        device.setInputMidiDevice(def.getInputMidiDevice());
                    }
                    if (device.getOutputMidiDevice() == null) {
                        device.setOutputMidiDevice(def.getOutputMidiDevice());
                    }
                });
    }

    /**
     * Take all macro definition of a {@link ProjectDevice} and forge a list of MidiRequestSequence to execute them
     *
     * @param device A device declared in the configuration of a project
     * @return List of {@link MidiRequestSequence} to execute
     */
    private List<MidiRequestSequence> forgeMidiRequestSequences(ProjectDevice device) {
        return device.getDumpRequests()
                .stream()
                .map(rawRequestDefinition -> forgeMidiRequestSequence(device, rawRequestDefinition)
                )
                .toList();
    }

    /**
     * Convert a macro definition to a {@link MidiRequestSequence}
     * <p>This method is public because we use it if a macro is passed from command line</p>
     *
     * @param device               The device that should receive this midi request
     * @param rawRequestDefinition Definition of the macro
     * @return The {@link MidiRequestSequence}
     */
    public MidiRequestSequence forgeMidiRequestSequence(ProjectDevice device, String rawRequestDefinition) {
        return CommandCall.parse(configFile, rawRequestDefinition)
                .stream()
                .map(commandCall -> midiDeviceLibrary.forgeMidiRequestSequence(configFile, device.getName(),
                        midiDeviceLibrary.getDevice(device.getName())
                                .map(d -> d.getMacro(commandCall))
                                .orElseThrow(), commandCall))
                .findFirst()
                .orElseThrow();

    }

    public Optional<MidiDeviceDefinition> getLibraryDeviceFromMidiPort(String midiPort) {
        loadLibary();
        return midiDeviceLibrary.getDeviceFromMidiPort(midiPort);
    }

    public Optional<ProjectDevice> getDefaultProjectDevice(String deviceName) {
        loadLibary();
        return midiDeviceLibrary.getDevice(deviceName)
                .map(this::forgeDefaultProjectDevice);
    }

    private ProjectDevice forgeDefaultProjectDevice(MidiDeviceDefinition midiDeviceDefinition) {
        ProjectDevice pd = new ProjectDevice();
        pd.setName(midiDeviceDefinition.getDeviceName());
        pd.setOutputBandWidth(midiDeviceDefinition.getOutputBandWidth());
        pd.setInputMidiDevice(midiDeviceDefinition.getInputMidiDevice());
        pd.setOutputMidiDevice(midiDeviceDefinition.getOutputMidiDevice());
        pd.setSysExPauseMs(midiDeviceDefinition.getSysExPauseMs());
        pd.setInactivityTimeoutMs(midiDeviceDefinition.getInactivityTimeoutMs());
        return pd;
    }
}
