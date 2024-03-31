package com.hypercube.workshop.syntheditor.infra.provider;

import com.hypercube.workshop.midiworkshop.common.MidiDeviceManager;
import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDevicesDTO;
import com.hypercube.workshop.syntheditor.infra.provider.mapper.MidiDevicesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final MidiDevicesMapper midiDevicesMapper;

    @GetMapping("/devices")
    MidiDevicesDTO getAllDevices() {
        MidiDeviceManager m = new MidiDeviceManager();
        m.collectDevices();
        return midiDevicesMapper.toDTO(m.getInputs(), m.getOutputs());
    }
}
