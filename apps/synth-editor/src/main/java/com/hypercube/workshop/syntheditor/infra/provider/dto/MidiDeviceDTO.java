package com.hypercube.workshop.syntheditor.infra.provider.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiDeviceDTO {
    private String name;
    private MidiDeviceType type;
}
