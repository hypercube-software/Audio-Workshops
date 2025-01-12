package com.hypercube.midi.translator.config.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.MidiBackupTranslator;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.yaml.MidiTranslationDeserializer;
import com.hypercube.midi.translator.error.ConfigError;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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
                            forgeMidiRequestsSequence(projectDevice);
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

    private void forgeMidiRequestsSequence(ProjectDevice device) {
        device.setDumpRequestTemplates(device.getDumpRequests()
                .stream()
                .map(requestDefinition ->
                        midiDeviceLibrary.forgeMidiRequestsSequence(configFile, device.getName(), requestDefinition)
                )
                .toList());
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
