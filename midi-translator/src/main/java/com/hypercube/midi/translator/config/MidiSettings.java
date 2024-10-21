package com.hypercube.midi.translator.config;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MidiSettings {

    private List<MidiPreset> selectedPresets;


}
