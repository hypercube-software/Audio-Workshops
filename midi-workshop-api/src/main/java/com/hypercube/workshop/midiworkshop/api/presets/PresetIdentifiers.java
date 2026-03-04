package com.hypercube.workshop.midiworkshop.api.presets;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PresetIdentifiers {
    private Integer msb;
    private Integer lsb;
    private Integer prg;
}
