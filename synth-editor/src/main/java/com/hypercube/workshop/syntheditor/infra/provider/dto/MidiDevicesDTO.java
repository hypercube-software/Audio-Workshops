package com.hypercube.workshop.syntheditor.infra.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MidiDevicesDTO {
    List<MidiDeviceDTO> inputs;
    List<MidiDeviceDTO> outputs;
}
