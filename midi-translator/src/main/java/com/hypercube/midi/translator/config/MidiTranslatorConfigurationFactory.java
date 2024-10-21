package com.hypercube.midi.translator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hypercube.midi.translator.error.ConfigError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class MidiTranslatorConfigurationFactory {
    @Value("${midi-translator-config:./config/config.yml}")
    private File configFile;

    @Bean
    public MidiTranslatorConfiguration loadConfig() {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(MidiTranslation.class, new MidiTranslationDeserializer());
            mapper.registerModule(module);
            MidiTranslatorConfiguration config = mapper.readValue(configFile, MidiTranslatorConfiguration.class);
            config.getTranslations()
                    .forEach(t -> config.getTranslationsMap()
                            .put(t.getCc(), t));
            return config;
        } catch (IOException e) {
            throw new ConfigError(e);
        }
    }
}
