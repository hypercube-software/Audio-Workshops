package com.hypercube.midi.translator.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class MidiTranslatorConfiguration {
    private DevicesSettings devices;
    private List<MidiTranslation> translations;
    private Map<Integer, MidiTranslation> translationsMap = new HashMap<>();
}
