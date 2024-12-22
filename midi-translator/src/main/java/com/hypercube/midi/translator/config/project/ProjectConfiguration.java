package com.hypercube.midi.translator.config.project;

import com.hypercube.midi.translator.config.project.device.ProjectDevice;
import com.hypercube.midi.translator.config.project.translation.MidiTranslation;
import com.hypercube.midi.translator.config.project.translation.TranslationSetting;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class ProjectConfiguration {
    private List<ProjectDevice> devices;
    private TranslationSetting translate;
    private List<MidiTranslation> translations;
    private Map<Integer, MidiTranslation> translationsMap = new HashMap<>();
}
