package com.hypercube.mpm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.mpm.MidiPresetManagerApplication;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.yaml.ObservableSerializer;
import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.common.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.common.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationFactory {
    private final MidiDeviceLibrary midiDeviceLibrary;

    @Value("${mpm-config:./config.yml}")
    private File configFile;

    private Favorites favorites;

    @Bean
    public ProjectConfiguration loadConfig() {
        loadLibary();
        favorites = loadFavoritePatches();
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
                config.setMidiDeviceLibrary(midiDeviceLibrary);
                config.setMidiDeviceManager(new MidiDeviceManager());
                config.getMidiDeviceManager()
                        .collectDevices();
                return config;
            } catch (IOException e) {
                throw new ConfigError(e);
            }
        } else {
            log.warn("The project config does not exists: " + configFile.getAbsolutePath());
            ProjectConfiguration config = new ProjectConfiguration();
            config.setMidiDeviceLibrary(midiDeviceLibrary);
            config.setMidiDeviceManager(new MidiDeviceManager());
            config.getMidiDeviceManager()
                    .collectDevices();
            return config;
        }
    }

    public void updateFavorites(Patch patch) {
        favorites.updateFavorites(patch);
        saveFavoritePatches(favorites);
    }

    public Patch getFavorite(Patch patch) {
        int idx = favorites.getFavorites()
                .indexOf(patch);
        if (idx != -1) {
            patch.setScore(favorites.getFavorites()
                    .get(idx)
                    .getScore());
        }
        return patch;
    }

    private void saveFavoritePatches(Favorites favorites) {
        File file = getFavoriteFile();
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule observableModule = new SimpleModule("observableModule");
            observableModule.setSerializerModifier(new ObservableSerializer());
            mapper.registerModule(observableModule);
            mapper.writeValue(file, favorites);
        } catch (IOException e) {
            throw new ConfigError(e);
        }
    }

    private File getFavoriteFile() {
        return new File(configFile.getParentFile(), "favorite-patches.yaml");
    }

    private Favorites loadFavoritePatches() {
        File file = getFavoriteFile();
        if (file.exists() && file.length() > 0) {
            var mapper = new ObjectMapper(new YAMLFactory());
            try {
                return mapper.readValue(file, Favorites.class);
            } catch (IOException e) {
                throw new ConfigError(e);
            }
        } else {
            return new Favorites();
        }
    }

    private void loadLibary() {
        if (!midiDeviceLibrary.isLoaded()) {
            midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(MidiPresetManagerApplication.class));
            midiDeviceLibrary.getDevices()
                    .values()
                    .forEach(d -> midiDeviceLibrary.collectCustomPatches(d));
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
