package com.hypercube.mpm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.mpm.MidiPresetManagerApplication;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationFactory {
    private final MidiDeviceLibrary midiDeviceLibrary;

    @Value("${mpm-config:./config.yml}")
    private File configFile;

    @Bean
    public ProjectConfiguration loadConfig() {
        loadLibary();
        if (configFile.exists()) {
            log.info("Loading project configuration from  %s...".formatted(configFile.getAbsolutePath()));
            var mapper = new ObjectMapper(new YAMLFactory());
            try {
                SimpleModule module = new SimpleModule();
                ProjectConfiguration config = mapper.readValue(configFile, ProjectConfiguration.class);
                config.getDevices()
                        .forEach(projectDevice -> {
                            setDefaultDeviceSettings(projectDevice);
                        });
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
            midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(MidiPresetManagerApplication.class));
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
