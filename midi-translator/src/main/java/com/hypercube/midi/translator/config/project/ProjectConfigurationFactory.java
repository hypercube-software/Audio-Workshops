package com.hypercube.midi.translator.config.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.config.lib.MidiDeviceLibrary;
import com.hypercube.midi.translator.config.project.device.DumpRequestTemplate;
import com.hypercube.midi.translator.config.project.device.ProjectDevice;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.yaml.DumpRequestTemplateDeserializer;
import com.hypercube.midi.translator.config.yaml.MidiTranslationDeserializer;
import com.hypercube.midi.translator.error.ConfigError;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ProjectConfigurationFactory {
    private final MidiDeviceLibrary midiDeviceLibrary;

    @Value("${midi-translator-config:./config.yml}")
    private File configFile;

    @Bean
    public ProjectConfiguration loadConfig() {
        midiDeviceLibrary.load();
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(MidiTranslation.class, new MidiTranslationDeserializer());
            module.addDeserializer(DumpRequestTemplate.class, new DumpRequestTemplateDeserializer(midiDeviceLibrary));
            mapper.registerModule(module);
            ProjectConfiguration config = mapper.readValue(configFile, ProjectConfiguration.class);
            config.getDevices()
                    .forEach(this::setDefaultDeviceSettings);
            config.getTranslations()
                    .forEach(t -> config.getTranslationsMap()
                            .put(t.getCc(), t));
            return config;
        } catch (IOException e) {
            throw new ConfigError(e);
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
}