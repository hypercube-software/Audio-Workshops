package com.hypercube.mpm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.mpm.MidiPresetManagerApplication;
import com.hypercube.mpm.model.Patch;
import com.hypercube.util.javafx.yaml.ObservableSerializer;
import com.hypercube.workshop.midiworkshop.api.MidiDeviceManager;
import com.hypercube.workshop.midiworkshop.api.config.ConfigHelper;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
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
        try {
            if (!configFile.exists() || configFile.length() == 0) {
                initEmptyConfig();
            }
            log.info("Loading project configuration from  %s...".formatted(configFile.getAbsolutePath()));
            var mapper = new ObjectMapper(new YAMLFactory());
            ProjectConfiguration config = mapper.readValue(configFile, ProjectConfiguration.class);
            config.setMidiDeviceLibrary(midiDeviceLibrary);
            config.setMidiDeviceManager(new MidiDeviceManager());
            config.getMidiDeviceManager()
                    .collectDevices();
            return config;
        } catch (IOException e) {
            throw new ConfigError(e);
        }
    }

    public void saveConfig(ProjectConfiguration configuration) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule observableModule = new SimpleModule("observableModule");
            observableModule.setSerializerModifier(new ObservableSerializer());
            mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            mapper.registerModule(observableModule);
            mapper.writeValue(configFile, configuration);
        } catch (IOException e) {
            throw new ConfigError(e);
        }
    }

    public void updateFavorites(Patch patch) {
        favorites.updateFavorites(patch);
        saveFavoritePatches(favorites);
    }

    public Patch getScoredPatchFromFavorite(Patch patch) {
        int idx = favorites.getFavorites()
                .indexOf(patch);
        if (idx != -1) {
            patch.setScore(favorites.getFavorites()
                    .get(idx)
                    .getScore());
        }
        return patch;
    }

    public void forceLoadLibrary() {
        midiDeviceLibrary.load(ConfigHelper.getApplicationFolder(MidiPresetManagerApplication.class));
        midiDeviceLibrary.getDevices()
                .values()
                .forEach(d -> midiDeviceLibrary.collectCustomPatches(d));
    }

    private void initEmptyConfig() throws IOException {
        ProjectConfiguration emptyConfig = new ProjectConfiguration();
        saveConfig(emptyConfig);
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
        return new File(ConfigHelper.getApplicationFolder(MidiPresetManagerApplication.class), "favorite-patches.yaml");
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
            forceLoadLibrary();
        }
    }
}
