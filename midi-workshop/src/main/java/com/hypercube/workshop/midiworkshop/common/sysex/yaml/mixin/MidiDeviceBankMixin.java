package com.hypercube.workshop.midiworkshop.common.sysex.yaml.mixin;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDevicePreset;

import java.util.List;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,      // N'auto-détecte aucun champ
        getterVisibility = JsonAutoDetect.Visibility.NONE,     // N'auto-détecte aucun getter
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,   // N'auto-détecte aucun getter de type 'is'
        setterVisibility = JsonAutoDetect.Visibility.NONE,     // N'auto-détecte aucun setter
        creatorVisibility = JsonAutoDetect.Visibility.NONE     // N'auto-détecte aucun constructeur/méthode factory
)
public class MidiDeviceBankMixin {
    @JsonProperty("presets")
    private List<MidiDevicePreset> presets;
}
