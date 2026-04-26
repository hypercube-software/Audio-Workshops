package com.hypercube.workshop.syntheditor.infra.provider.mapper;

import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDeviceDTO;
import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDevicesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MidiDevicesMapper {

    default MidiDevicesDTO toDTO(List<MidiInPort> inputs, List<MidiOutPort> outputs) {
        return new MidiDevicesDTO(toInDTO(inputs), toOutDTO(outputs));
    }

    List<MidiDeviceDTO> toOutDTO(List<MidiOutPort> outputs);

    List<MidiDeviceDTO> toInDTO(List<MidiInPort> inputs);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", constant = "INPUT")
    MidiDeviceDTO toDTO(MidiInPort input);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", constant = "INPUT")
    MidiDeviceDTO toDTO(MidiOutPort output);
}
