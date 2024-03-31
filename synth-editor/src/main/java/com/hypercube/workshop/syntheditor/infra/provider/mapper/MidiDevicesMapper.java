package com.hypercube.workshop.syntheditor.infra.provider.mapper;

import com.hypercube.workshop.midiworkshop.common.MidiInDevice;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDeviceDTO;
import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDevicesDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MidiDevicesMapper {

    default MidiDevicesDTO toDTO(List<MidiInDevice> inputs, List<MidiOutDevice> outputs) {
        return new MidiDevicesDTO(toInDTO(inputs), toOutDTO(outputs));
    }

    List<MidiDeviceDTO> toOutDTO(List<MidiOutDevice> outputs);

    List<MidiDeviceDTO> toInDTO(List<MidiInDevice> inputs);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", constant = "INPUT")
    MidiDeviceDTO toDTO(MidiInDevice input);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", constant = "INPUT")
    MidiDeviceDTO toDTO(MidiOutDevice output);
}
