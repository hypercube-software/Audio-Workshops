package com.hypercube.midi.translator.config.project.translation;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TranslationSetting {
    private String fromMidiDevice;
    private String toDevice;
}
