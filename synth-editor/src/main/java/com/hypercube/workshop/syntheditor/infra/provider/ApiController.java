package com.hypercube.workshop.syntheditor.infra.provider;

import com.hypercube.workshop.syntheditor.infra.provider.dto.MidiDevicesDTO;
import com.hypercube.workshop.syntheditor.infra.provider.mapper.MidiDevicesMapper;
import com.hypercube.workshop.syntheditor.model.SynthEditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final MidiDevicesMapper midiDevicesMapper;
    private final SynthEditorService synthEditorService;

    @GetMapping("/devices")
    MidiDevicesDTO getAllDevices() {
        var deviceMgr = synthEditorService.getMidiDeviceManager();
        return midiDevicesMapper.toDTO(deviceMgr.getInputs(), deviceMgr.getOutputs());
    }

    @GetMapping("/input/{deviceName}")
    void setInput(@PathVariable("deviceName") String deviceName) {
        synthEditorService.changeInput(deviceName);
    }

    @GetMapping("/output/{deviceName}")
    void setOutput(@PathVariable("deviceName") String deviceName) {
        synthEditorService.changeOutput(deviceName);
    }
}
